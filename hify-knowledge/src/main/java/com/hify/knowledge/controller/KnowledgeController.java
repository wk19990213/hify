package com.hify.knowledge.controller;

import com.hify.common.result.PageResult;
import com.hify.common.result.Result;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import com.hify.knowledge.dto.DocumentResp;
import com.hify.knowledge.dto.KnowledgeBaseResp;
import com.hify.knowledge.dto.RagResp;
import com.hify.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/bases")
    public Result<KnowledgeBaseResp> createKB(@RequestParam("name") String name,
                                              @RequestParam(name = "description", defaultValue = "") String description) {
        return Result.ok(knowledgeService.createKB(name, description));
    }

    @GetMapping("/bases")
    public Result<PageResult<KnowledgeBaseResp>> listKB(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                                         @RequestParam(name = "pageSize", defaultValue = "20") Integer size) {
        return Result.ok(knowledgeService.listKB(page, size));
    }

    @DeleteMapping("/bases/{id}")
    public Result<Void> deleteKB(@PathVariable("id") Long id) {
        knowledgeService.deleteKB(id);
        return Result.ok();
    }

    @RateLimiter(name = "knowledge-upload-rate-limiter")
    @PostMapping("/bases/{kbId}/documents")
    public Result<DocumentResp> uploadDocument(@PathVariable("kbId") Long kbId,
                                                @RequestParam("file") MultipartFile file) {
        return Result.ok(knowledgeService.uploadDocument(kbId, file));
    }

    @PostMapping("/bases/{kbId}/query")
    public Result<RagResp> query(@PathVariable("kbId") Long kbId,
                                  @RequestParam("question") String question) {
        return Result.ok(knowledgeService.query(kbId, question));
    }
}
