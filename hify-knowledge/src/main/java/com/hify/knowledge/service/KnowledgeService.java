package com.hify.knowledge.service;

import com.hify.knowledge.dto.*;
import com.hify.common.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService {
    KnowledgeBaseResp createKB(String name, String description);
    PageResult<KnowledgeBaseResp> listKB(Integer page, Integer size);
    void deleteKB(Long id);
    DocumentResp uploadDocument(Long kbId, MultipartFile file);
    RagResp query(Long kbId, String question);
}
