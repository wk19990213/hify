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

    /** 主键（使用 provider_id 作为主键） */
    @TableId
    private Long providerId;

    /** 状态：HEALTHY/DEGRADED/UNHEALTHY/UNKNOWN */
    private String status;

    /** 最后检查时间 */
    private LocalDateTime lastCheckTime;

    /** 最后成功时间 */
    private LocalDateTime lastSuccessTime;

    /** 最后错误时间 */
    private LocalDateTime lastErrorTime;

    /** 连续失败次数 */
    private Integer consecutiveFailures;

    /** 平均延迟毫秒 */
    private Integer avgLatencyMs;

    /** 成功率（百分比，如 99.50） */
    private java.math.BigDecimal successRate;

    /** 最后错误信息 */
    private String lastErrorMsg;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
