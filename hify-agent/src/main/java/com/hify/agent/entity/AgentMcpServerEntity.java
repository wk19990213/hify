package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_mcp_server")
public class AgentMcpServerEntity extends BaseEntity {

    private Long agentId;
    private Long mcpServerId;
    private Integer sortOrder;
}
