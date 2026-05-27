package com.hify.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.common.exception.BizException;
import com.hify.common.result.PageResult;
import com.hify.common.util.PageHelper;
import com.hify.knowledge.dto.*;
import com.hify.knowledge.entity.*;
import com.hify.knowledge.converter.KnowledgeConverter;
import com.hify.knowledge.mapper.*;
import com.hify.knowledge.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeBaseMapper kbMapper;
    private final DocumentMapper docMapper;
    private final DocumentChunkMapper chunkMapper;
    private final TextSplitter textSplitter;
    private final EmbeddingService embeddingService;
    private final DocumentParserService documentParserService;
    private final FileValidationService fileValidationService;
    private final RagQueryService ragQueryService;

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
        fileValidationService.validate(file);

        KnowledgeBaseEntity kb = kbMapper.selectById(kbId);
        if (kb == null || kb.getDeleted() == 1)
            throw BizException.notFound("知识库不存在");

        String fileName = file.getOriginalFilename();
        String fileType = fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "txt";

        String text = documentParserService.parseDocument(file, fileType);
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
        return ragQueryService.query(kbId, question);
    }

    private KnowledgeBaseResp toKBResp(KnowledgeBaseEntity kb) {
        return KnowledgeConverter.INSTANCE.toKBResp(kb);
    }

    private DocumentResp toDocResp(DocumentEntity doc) {
        return KnowledgeConverter.INSTANCE.toDocResp(doc);
    }
}
