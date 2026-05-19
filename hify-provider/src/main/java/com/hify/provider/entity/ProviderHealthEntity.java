package com.hify.provider.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商健康状态实体
 */
@Data
@TableName("provider_health")
public class ProviderHealthEntity {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 供应商 ID
     */
    private Long providerId;

    /**
     * 状态：HEALTHY/DEGRADED/UNHEALTHY/UNKNOWN
     */
    private String status;

    /**
     * 连续失败次数
     */
    private Integer failCount;

    /**
     * 延迟毫秒
     */
    private Integer latencyMs;

    /**
     * 最后成功时间
     */
    private LocalDateTime lastSuccessAt;

    /**
     * 最后错误信息
     */
    private String lastErrorMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
