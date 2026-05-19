package com.hify.agent.dto;

import lombok.Data;

/**
 * Agent 列表查询参数
 */
@Data
public class AgentListParams {

    /**
     * 页码，从 1 开始
     */
    private Integer page = 1;

    /**
     * 每页条数，默认 20，最大 100
     */
    private Integer pageSize = 20;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式：asc / desc
     */
    private String sortOrder;

    /**
     * 名称模糊查询
     */
    private String name;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

    /**
     * 模型配置 ID
     */
    private Long modelConfigId;
}
