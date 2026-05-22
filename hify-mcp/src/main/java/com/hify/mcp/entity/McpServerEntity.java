package com.hify.mcp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_server")
public class McpServerEntity extends BaseEntity {

    private String name;
    private String url;
    private String authConfig;
    private String transportType;
    private Integer status;
}
