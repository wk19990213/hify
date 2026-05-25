package com.hify.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.crypto.AesEncryptor;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.common.util.LogSanitizer;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.knowledge.dto.*;
import com.hify.knowledge.entity.*;
import com.hify.knowledge.mapper.*;
import com.hify.knowledge.service.*;
import com.hify.provider.adapter.*;
import com.hify.provider.entity.ModelConfigEntity;
import com.hify.provider.entity.ProviderEntity;
import com.hify.provider.entity.ProviderModelEntity;
import com.hify.provider.mapper.ModelConfigMapper;
import com.hify.provider.mapper.ProviderMapper;
import com.hify.provider.mapper.ProviderModelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    // 允许的文件类型白名单
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("pdf", "docx", "txt", "md");
    // 最大文件大小：50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    private final KnowledgeBaseMapper kbMapper;
    private final DocumentMapper docMapper;
    private final DocumentChunkMapper chunkMapper;
    private final TextSplitter textSplitter;
    private final EmbeddingService embeddingService;
    private final ProviderMapper providerMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final ProviderModelMapper providerModelMapper;
    private final ProviderAdapterFactory adapterFactory;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public KnowledgeBaseResp createKB(String name, String description) {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setName(name);
        kb.setDescription(description != null ? description : "");
        kb.setEmbeddingModel("BAAI/bge-m3");
        kb.setChunkSize(800);
        kb.setChunkOverlap(100);
        kb.setStatus(1);
        kbMapper.insert(kb);
        return toKBResp(kb);
    }

    @Override
    public PageResult<KnowledgeBaseResp> listKB(Integer page, Integer size) {
        var p = PageHelper.<KnowledgeBaseEntity>toPage(page, size);
        var wrapper = new LambdaQueryWrapper<KnowledgeBaseEntity>()
                .eq(KnowledgeBaseEntity::getDeleted, 0)
                .orderByDesc(KnowledgeBaseEntity::getCreatedAt);
        var result = kbMapper.selectPage(p, wrapper);
        var list = result.getRecords().stream().map(kb -> {
            var resp = toKBResp(kb);
            Long count = docMapper.selectCount(new LambdaQueryWrapper<DocumentEntity>()
                    .eq(DocumentEntity::getKbId, kb.getId())
                    .eq(DocumentEntity::getDeleted, 0));
            resp.setDocumentCount(count.intValue());
            return resp;
        }).toList();
        return PageResult.ok(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void deleteKB(Long id) {
        kbMapper.deleteById(id);
    }

    @Override
    @Transactional
    public DocumentResp uploadDocument(Long kbId, MultipartFile file) {
        // 文件上传安全验证
        validateUploadFile(file);

        KnowledgeBaseEntity kb = kbMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() == 1)
            throw BizException.notFound("知识库不存在");

        String fileName = file.getOriginalFilename();
        String fileType = fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "txt";

        String text = parseDocument(file, fileType);
        if (text == null || text.isBlank())
            throw BizException.paramError("无法解析文档内容");

        DocumentEntity doc = new DocumentEntity();
        doc.setKbId(kbId);
        doc.setName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setStatus("processing");
        docMapper.insert(doc);

        List<TextSplitter.Chunk> chunks = textSplitter.split(text, kb.getChunkSize(), kb.getChunkOverlap());
        for (TextSplitter.Chunk chunk : chunks) {
            float[] vec = embeddingService.embed(chunk.content(), kb.getEmbeddingModel());
            DocumentChunkEntity c = new DocumentChunkEntity();
            c.setDocId(doc.getId());
            c.setChunkIndex(chunk.index());
            c.setContent(chunk.content());
            c.setEmbedding(embeddingService.vecToString(vec));
            chunkMapper.insert(c);
        }

        doc.setStatus("completed");
        doc.setChunkCount(chunks.size());
        docMapper.updateById(doc);
        return toDocResp(doc);
    }

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

        String prompt = String.format(
                "你是一个知识库助手。请根据以下参考资料回答用户问题。如果参考资料中没有相关信息，请如实告知。\n\n参考资料：\n%s\n\n用户问题：%s\n\n回答：",
                context, question);

        // 查找有可用提供商的模型配置
        Page<ModelConfigEntity> mcPage = modelConfigMapper.selectPage(
                Page.of(1, 1),
                new LambdaQueryWrapper<ModelConfigEntity>()
                        .eq(ModelConfigEntity::getStatus, 1)
                        .eq(ModelConfigEntity::getDeleted, 0)
                        .gt(ModelConfigEntity::getProviderCount, 0));
        ProviderEntity provider = null;
        ModelConfigEntity modelConfig = mcPage.getRecords().isEmpty() ? null : mcPage.getRecords().get(0);
        if (modelConfig != null) {
            // 通过 provider_model 查找可用 Provider
            List<ProviderModelEntity> pmList = providerModelMapper.selectList(
                    new LambdaQueryWrapper<ProviderModelEntity>()
                            .eq(ProviderModelEntity::getModelId, modelConfig.getModelId()));
            for (ProviderModelEntity pm : pmList) {
                ProviderEntity p = providerMapper.selectById(pm.getProviderId());
                if (p != null && p.getDeleted() == 0 && p.getStatus() == 1) {
                    provider = p;
                    break;
                }
            }
        }
        if (provider == null) throw BizException.paramError("没有可用的 LLM Provider");

        ProviderAdapter adapter = adapterFactory.getAdapter(provider.getType());
        Map<String, Object> authConfig = null;
        try {
            String authJson = AesEncryptor.decrypt(provider.getAuthConfig());
            authConfig = objectMapper.readValue(authJson, Map.class);
        } catch (Exception ignored) {}

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

    private String parseDocument(MultipartFile file, String fileType) {
        try (InputStream in = file.getInputStream()) {
            return switch (fileType) {
                case "pdf" -> parsePdf(in);
                case "docx" -> parseDocx(in);
                default -> new String(in.readAllBytes());
            };
        } catch (Exception e) {
            log.error("Document parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String parsePdf(InputStream in) throws Exception {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    private String parseDocx(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    private KnowledgeBaseResp toKBResp(KnowledgeBaseEntity kb) {
        KnowledgeBaseResp r = new KnowledgeBaseResp();
        BeanUtils.copyProperties(kb, r);
        return r;
    }

    private DocumentResp toDocResp(DocumentEntity doc) {
        DocumentResp r = new DocumentResp();
        BeanUtils.copyProperties(doc, r);
        return r;
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

    /**
     * 验证上传文件的安全性
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.paramError("文件不能为空");
        }

        // 1. 文件大小限制
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.paramError("文件大小超过限制（最大50MB）");
        }

        // 2. 文件类型白名单验证
        String fileName = file.getOriginalFilename();
        String fileType = fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";

        if (!ALLOWED_FILE_TYPES.contains(fileType)) {
            throw BizException.paramError("不支持的文件类型，仅支持：pdf, docx, txt, md");
        }

        // 3. 文件名安全检查：禁止路径遍历
        if (fileName != null && (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\"))) {
            throw BizException.paramError("文件名包含非法字符");
        }

        // 4. 魔数检查（可选，针对PDF和DOCX）
        validateFileMagicNumber(file, fileType);
    }

    /**
     * 验证文件魔数，防止伪造扩展名
     */
    private void validateFileMagicNumber(MultipartFile file, String fileType) {
        try (InputStream is = file.getInputStream()) {
            byte[] magic = new byte[4];
            int read = is.read(magic);
            if (read < 4) return; // 文件太小，无法检查

            // PDF: %PDF (0x25 0x50 0x44 0x46)
            if ("pdf".equals(fileType)) {
                if (magic[0] != 0x25 || magic[1] != 0x50 || magic[2] != 0x44 || magic[3] != 0x46) {
                    throw BizException.paramError("文件内容不是有效的PDF格式");
                }
            }
            // DOCX: PK (ZIP格式，0x50 0x4B)
            else if ("docx".equals(fileType)) {
                if (magic[0] != 0x50 || magic[1] != 0x4B) {
                    throw BizException.paramError("文件内容不是有效的DOCX格式");
                }
            }
        } catch (Exception e) {
            log.error("文件魔数检查失败", e);
            throw BizException.paramError("文件验证失败");
        }
    }
}
