-- Hify Provider 模块数据模型
-- 供应商管理、模型管理、健康状态

-- 供应商配置表
CREATE TABLE provider (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(100)    NOT NULL COMMENT '显示名称，如"OpenAI官方"',
    code            VARCHAR(50)     NOT NULL UNIQUE COMMENT '唯一编码，如"openai-prod"',

    -- 供应商类型
    type            VARCHAR(20)     NOT NULL COMMENT '类型：openai/anthropic/ollama/openai_compatible',

    -- 连接配置
    base_url        VARCHAR(500)    NOT NULL COMMENT 'API Base URL',

    -- 鉴权信息（JSON灵活存储，支持多种方式）
    auth_config     JSON            NOT NULL COMMENT '鉴权配置JSON',

    -- 请求配置
    timeout_ms      INT             DEFAULT 30000 COMMENT '请求超时毫秒',
    max_retries     TINYINT         DEFAULT 3 COMMENT '最大重试次数',
    retry_interval_ms INT           DEFAULT 1000 COMMENT '重试间隔毫秒',

    -- 状态
    status          TINYINT         DEFAULT 1 COMMENT '0禁用 1启用 2故障',
    sort_order      INT             DEFAULT 0 COMMENT '排序',

    -- 扩展配置
    extra_config    JSON            NULL COMMENT '额外配置，如headers、代理等',

    -- 标准字段
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    INDEX idx_type_status (type, status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型提供商配置';


-- 模型实例表
CREATE TABLE model (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    provider_id     BIGINT          NOT NULL COMMENT '所属供应商ID',

    -- 模型标识
    model_name      VARCHAR(100)    NOT NULL COMMENT '原始模型名，如gpt-4-turbo',
    display_name    VARCHAR(100)    NOT NULL COMMENT '显示名称，如"GPT-4 Turbo"',
    code            VARCHAR(50)     NOT NULL COMMENT '唯一编码，如"gpt4t"',

    -- 能力配置
    capabilities    JSON            NOT NULL COMMENT '能力配置JSON',

    -- 成本配置（用于统计和限额）
    price_config    JSON            NULL COMMENT '计费配置JSON',

    -- 状态
    status          TINYINT         DEFAULT 1 COMMENT '0禁用 1启用 2 deprecated',
    is_default      TINYINT(1)      DEFAULT 0 COMMENT '是否默认模型',

    -- 排序
    sort_order      INT             DEFAULT 0,

    -- 标准字段
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_model (provider_id, model_name, deleted),
    INDEX idx_provider_status (provider_id, status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型实例';


-- 供应商健康状态表
CREATE TABLE provider_health (
    provider_id     BIGINT          NOT NULL COMMENT '供应商ID',

    -- 状态
    status          VARCHAR(20)     NOT NULL DEFAULT 'UNKNOWN'
                                    COMMENT 'HEALTHY/DEGRADED/UNHEALTHY/UNKNOWN',

    -- 健康指标
    last_check_time DATETIME(3)     NULL COMMENT '最后检测时间',
    last_success_time DATETIME(3)   NULL COMMENT '最后成功时间',
    last_error_time DATETIME(3)     NULL COMMENT '最后失败时间',
    consecutive_failures INT         DEFAULT 0 COMMENT '连续失败次数',

    -- 性能指标
    avg_latency_ms  INT             NULL COMMENT '平均延迟毫秒',
    success_rate    DECIMAL(5,2)    NULL COMMENT '成功率百分比',

    -- 错误信息
    last_error_msg  VARCHAR(500)    NULL COMMENT '最后错误信息',

    -- 标准字段
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (provider_id),
    INDEX idx_status (status),
    INDEX idx_check_time (last_check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商健康状态';
