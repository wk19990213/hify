package com.hify.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.common.exception.BizException;
import com.hify.knowledge.dto.RagResp;
import com.hify.knowledge.entity.DocumentChunkEntity;
import com.hify.knowledge.entity.DocumentEntity;
import com.hify.knowledge.entity.KnowledgeBaseEntity;
import com.hify.knowledge.mapper.DocumentChunkMapper;
import com.hify.knowledge.mapper.DocumentMapper;
import com.hify.knowledge.mapper.KnowledgeBaseMapper;
import com.hify.knowledge.service.EmbeddingService;
import com.hify.knowledge.service.RagQueryService;
import com.hify.provider.adapter.ChatRequest;
import com.hify.provider.adapter.ProviderAdapter;
import com.hify.provider.adapter.ProviderAdapterFactory;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.service.ProviderDiscoveryService;
import com.hify.provider.util.AuthConfigHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryServiceImpl implements RagQueryService {

    private final KnowledgeBaseMapper kbMapper;
    private final DocumentMapper docMapper;
    private final DocumentChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final ModelConfigMapper modelConfigMapper;
    private final ProviderDiscoveryService providerDiscoveryService;
    private final ProviderAdapterFactory adapterFactory;

    @Override
    public RagResp query(Long kbId, String question) {
        long start = System.currentTimeMillis();
        KnowledgeBaseEntity kb = kbMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() == 1)
            throw BizException.notFound("知识库不存在");

        float[] qVec = embeddingService.embed(question, kb.getEmbeddingModel());
        if (qVec.length == 0)
            throw BizException.paramError("Embedding 失败，请检查 Ollama 是否运行");

        List<String> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        searchAndBuildContext(kbId, qVec, sources, context);
        if (sources.isEmpty())
            throw BizException.paramError("知识库中没有找到相关文档");

        String prompt = buildRagPrompt(context.toString(), question);

        Page<ModelConfigEntity> mcPage = modelConfigMapper.selectPage(
                Page.of(1, 1),
                new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getStatus, 1)
                        .eq(ModelConfigEntity::getDeleted, 0)
                        .gt(ModelConfigEntity::getProviderCount, 0));
        ModelConfigEntity modelConfig = mcPage.getRecords().isEmpty() ? null : mcPage.getRecords().get(0);
        ProviderEntity provider = modelConfig != null
                ? providerDiscoveryService.findAvailableProviderByModelId(modelConfig.getModelId()) : null;
        if (provider == null) throw BizException.paramError("没有可用的 LLM Provider");

        ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());
        Map<String, Object> authConfig = AuthConfigHelper.decryptAuthConfig(provider.getAuthConfig());
        if (authConfig.isEmpty()) authConfig = null;

        String llmResp = adapter.chat(provider.getBaseUrl(), authConfig,
                new ChatRequest(modelConfig.getModelId(),
                        List.of(Map.of("role", "user", "content", prompt)), 0.7, false));

        RagResp resp = new RagResp();
        resp.setAnswer(adapter.extractContent(llmResp));
        resp.setSources(sources);
        resp.setTokenCount(adapter.extractTokenCount(llmResp));
        resp.setLatencyMs((int) (System.currentTimeMillis() - start));
        return resp;
    }

    private String buildRagPrompt(String context, String question) {
        return String.format(
                "你是一个知识库助手。请根据以下参考资料回答用户问题。如果参考资料中没有相关信息，请如实告知。\n\n参考资料：\n%s\n\n用户问题：%s\n\n回答：",
                context, question);
    }

    /** 向量检索相似分块并构建上下文 */
    private void searchAndBuildContext(Long kbId, float[] queryVec,
                                        List<String> sources, StringBuilder context) {
        List<Long> kbDocIds = docMapper.selectList(new LambdaQueryWrapper<DocumentEntity>()
                        .eq(DocumentEntity::getKbId, kbId).eq(DocumentEntity::getDeleted, 0))
                .stream().map(DocumentEntity::getId).toList();
        if (kbDocIds.isEmpty()) return;

        List<DocumentChunkEntity> allChunks = chunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunkEntity>()
                        .in(DocumentChunkEntity::getDocId, kbDocIds)
                        .eq(DocumentChunkEntity::getDeleted, 0));

        record ScoredChunk(DocumentChunkEntity chunk, double score) {}
        List<ScoredChunk> scored = new ArrayList<>();
        for (var c : allChunks) {
            float[] cVec = embeddingService.stringToVec(c.getEmbedding());
            if (cVec.length == 0) continue;
            double sim = EmbeddingService.cosineSimilarity(queryVec, cVec);
            scored.add(new ScoredChunk(c, sim));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<DocumentChunkEntity> topChunks = scored.stream()
                .limit(5).map(ScoredChunk::chunk).toList();

        for (var c : topChunks) {
            context.append(c.getContent()).append("\n\n");
            sources.add(c.getContent().substring(0, Math.min(100, c.getContent().length())) + "...");
        }
    }
}
