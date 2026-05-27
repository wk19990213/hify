package com.hify.knowledge.service;

import com.hify.knowledge.dto.RagResp;

/**
 * RAG 检索增强生成服务。
 * 负责向量检索、上下文构建、LLM 调用全链路。
 */
public interface RagQueryService {

    /**
     * 对指定知识库执行 RAG 查询。
     *
     * @param kbId     知识库 ID
     * @param question 用户问题
     * @return RAG 响应，包含回答、来源、Token 数和耗时
     */
    RagResp query(Long kbId, String question);
}
