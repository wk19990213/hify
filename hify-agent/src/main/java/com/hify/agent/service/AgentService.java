package com.hify.agent.service;

import com.hify.agent.dto.AgentListParams;
import com.hify.agent.dto.AgentRequest;
import com.hify.agent.dto.AgentResponse;
import com.hify.common.result.PageResult;

import com.hify.agent.dto.AgentToolResponse;

import java.util.List;

/**
 * Agent 服务接口
 */
public interface AgentService {

    /**
     * 创建 Agent
     */
    Long create(AgentRequest req);

    /**
     * 更新 Agent
     */
    void update(Long id, AgentRequest req);

    /**
     * 删除 Agent（逻辑删除，同时删除工具绑定）
     */
    void delete(Long id);

    /**
     * 分页列表（含模型配置名称和工具数量）
     */
    PageResult<AgentResponse> list(AgentListParams params);

    /**
     * 查看详情（含工具列表）
     */
    AgentResponse getDetail(Long id);

    /**
     * 获取 Agent 绑定的工具列表
     */
    List<AgentToolResponse> getAgentTools(Long agentId);

    /**
     * 批量更新 Agent 状态
     */
    void batchUpdateStatus(List<Long> ids, Integer status);
}
