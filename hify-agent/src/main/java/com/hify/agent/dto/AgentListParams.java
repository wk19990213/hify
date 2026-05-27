package com.hify.agent.dto;

import com.hify.common.dto.BasePageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 列表查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentListParams extends BasePageParams {

    /** 模型配置 ID */
    private Long modelConfigId;
}
