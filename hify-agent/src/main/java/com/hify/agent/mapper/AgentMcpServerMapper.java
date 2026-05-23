package com.hify.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.agent.entity.AgentMcpServerEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AgentMcpServerMapper extends BaseMapper<AgentMcpServerEntity> {

    /**
     * 插入或重新激活记录，绕过 @TableLogic 自动处理
     * 如果记录存在（包括已逻辑删除的），则更新 deleted=0 和 sort_order
     * 如果不存在，则插入新记录
     */
    @Insert("INSERT INTO agent_mcp_server (agent_id, mcp_server_id, sort_order, created_at, updated_at, deleted) "
          + "VALUES (#{agentId}, #{mcpServerId}, #{sortOrder}, NOW(3), NOW(3), 0) "
          + "ON DUPLICATE KEY UPDATE deleted = 0, sort_order = VALUES(sort_order), updated_at = NOW(3)")
    int insertOrReactivate(@Param("agentId") Long agentId,
                          @Param("mcpServerId") Long mcpServerId,
                          @Param("sortOrder") Integer sortOrder);

    /**
     * 软删除指定 Agent 的所有 MCP 服务绑定（设置 deleted=1）
     */
    @Update("UPDATE agent_mcp_server SET deleted = 1, updated_at = NOW(3) WHERE agent_id = #{agentId} AND deleted = 0")
    int softDeleteByAgentId(@Param("agentId") Long agentId);
}
