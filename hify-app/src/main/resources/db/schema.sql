-- ============================================
-- Hify 建表 DDL
-- 规范：主键 id BIGINT 自增、
--       时间字段 created_at/updated_at DATETIME(3)、
--       逻辑删除 deleted TINYINT(1) DEFAULT 0、
--       字符集 utf8mb4、引擎 InnoDB
-- ============================================

-- ----------------------------
-- 1. MCP 服务器配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS mcp_server (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           VARCHAR(64)   NOT NULL COMMENT '服务名称',
    command        VARCHAR(255)  DEFAULT NULL COMMENT '启动命令（stdio 传输）',
    args_json      TEXT          DEFAULT NULL COMMENT '命令参数（JSON 数组）',
    env_vars_json  TEXT          DEFAULT NULL COMMENT '环境变量（JSON 对象）',
    url            VARCHAR(255)  DEFAULT NULL COMMENT '服务 URL（sse/streamable-http 传输）',
    transport_type VARCHAR(32)   NOT NULL DEFAULT 'stdio' COMMENT '传输类型：stdio/sse/streamable-http',
    status         TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP 服务器配置';

-- ----------------------------
-- 2. 对话会话
-- ----------------------------
CREATE TABLE IF NOT EXISTS chat_session (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id   VARCHAR(64)   NOT NULL COMMENT '会话标识（UUID）',
    agent_id     BIGINT        NOT NULL COMMENT 'Agent ID',
    title        VARCHAR(255)  DEFAULT '' COMMENT '会话标题',
    status       VARCHAR(16)   NOT NULL DEFAULT 'active' COMMENT '状态：active/ended',
    context_json MEDIUMTEXT    DEFAULT NULL COMMENT '上下文快照（JSON）',
    created_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话';

-- ----------------------------
-- 3. 对话消息
-- ----------------------------
CREATE TABLE IF NOT EXISTS chat_message (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id    BIGINT        NOT NULL COMMENT '会话 ID',
    role          VARCHAR(16)   NOT NULL COMMENT '角色：user/assistant/system/tool',
    content       MEDIUMTEXT    DEFAULT NULL COMMENT '消息内容',
    token_count   INT           DEFAULT 0 COMMENT '消耗 token 数',
    metadata_json TEXT          DEFAULT NULL COMMENT '元数据（JSON）',
    created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    INDEX idx_session_created (session_id, deleted, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息';

-- ----------------------------
-- 4. 模型提供商
-- ----------------------------
CREATE TABLE IF NOT EXISTS provider (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(100)    NOT NULL COMMENT '显示名称，如"OpenAI官方"',
    code            VARCHAR(50)     NOT NULL COMMENT '唯一编码，如"openai-prod"',
    type            VARCHAR(20)     NOT NULL COMMENT '类型：openai/anthropic/ollama/openai_compatible',
    base_url        VARCHAR(500)    NOT NULL COMMENT 'API Base URL',
    auth_config     JSON            NOT NULL COMMENT '鉴权配置JSON',
    timeout_ms      INT             DEFAULT 30000 COMMENT '请求超时毫秒',
    max_retries     TINYINT         DEFAULT 3 COMMENT '最大重试次数',
    retry_interval_ms INT           DEFAULT 1000 COMMENT '重试间隔毫秒',
    status          TINYINT         DEFAULT 1 COMMENT '0禁用 1启用 2故障',
    sort_order      INT             DEFAULT 0 COMMENT '排序',
    extra_config    JSON            NULL COMMENT '额外配置，如headers、代理等',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    INDEX idx_type_status (type, status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型提供商配置';

-- ----------------------------
-- 5. 模型配置（按 model_id 唯一，与 provider 解耦）
-- ----------------------------
CREATE TABLE IF NOT EXISTS model_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    provider_id     BIGINT          NOT NULL COMMENT '代表性提供商 ID',
    model_id        VARCHAR(100)    NOT NULL COMMENT '原始模型标识，如 gpt-4-turbo',
    name            VARCHAR(100)    NOT NULL COMMENT '显示名称，如 GPT-4 Turbo',
    code            VARCHAR(50)     NOT NULL COMMENT '唯一编码，如"gpt4t"',
    capabilities    JSON            NULL COMMENT '能力配置JSON',
    price_config    JSON            NULL COMMENT '计费配置JSON',
    status          TINYINT         DEFAULT 1 COMMENT '0禁用 1启用 2 deprecated',
    is_default      TINYINT(1)      DEFAULT 0 COMMENT '是否默认模型',
    sort_order      INT             DEFAULT 0,
    provider_count  INT             NOT NULL DEFAULT 1 COMMENT '提供此模型的供应商数量',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_id (model_id, deleted),
    INDEX idx_provider_status (provider_id, status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置';

-- ----------------------------
-- 5.1 提供商-模型关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS provider_model (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    provider_id BIGINT       NOT NULL COMMENT '提供商 ID',
    model_id    VARCHAR(100) NOT NULL COMMENT '模型标识',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_pm (provider_id, model_id),
    INDEX idx_model_id (model_id),
    INDEX idx_provider_id (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提供商-模型关联表';

-- ----------------------------
-- 6. Agent 配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS agent (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name                VARCHAR(128)    NOT NULL COMMENT 'Agent 名称',
    code                VARCHAR(64)     NOT NULL COMMENT '唯一编码',
    description         VARCHAR(512)    DEFAULT '' COMMENT '描述',
    model_config_id     BIGINT          DEFAULT NULL COMMENT '模型配置 ID（可空）',
    system_prompt       MEDIUMTEXT      DEFAULT NULL COMMENT '系统提示词',
    conversation_max_rounds INT         DEFAULT 20 COMMENT '最大对话轮数',
    temperature         DECIMAL(3,2)    DEFAULT 0.70 COMMENT '温度参数 0-2',
    status              TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    sort_order          INT             DEFAULT 0 COMMENT '排序',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted             TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    INDEX idx_model_config (model_config_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 配置';

-- ----------------------------
-- 7. Agent 工具绑定
-- ----------------------------
CREATE TABLE IF NOT EXISTS agent_tool (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    agent_id        BIGINT          NOT NULL COMMENT 'Agent ID',
    tool_name       VARCHAR(64)     NOT NULL COMMENT '工具名称',
    tool_type       VARCHAR(16)     NOT NULL DEFAULT 'mcp' COMMENT '工具类型：mcp/builtin',
    mcp_server_id   BIGINT          DEFAULT NULL COMMENT 'MCP 服务 ID（tool_type=mcp 时）',
    config_json     JSON            DEFAULT NULL COMMENT '工具配置 JSON',
    sort_order      INT             DEFAULT 0 COMMENT '排序',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_tool (agent_id, tool_name, deleted),
    INDEX idx_agent (agent_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 工具绑定';

-- ----------------------------
-- 8. 供应商健康状态
-- ----------------------------
CREATE TABLE IF NOT EXISTS provider_health (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    provider_id     BIGINT          NOT NULL COMMENT '供应商 ID',
    status          VARCHAR(20)     NOT NULL DEFAULT 'UNKNOWN' COMMENT 'HEALTHY/DEGRADED/UNHEALTHY/UNKNOWN',
    fail_count      INT             DEFAULT 0 COMMENT '连续失败次数',
    latency_ms      INT             DEFAULT NULL COMMENT '延迟毫秒',
    last_success_at DATETIME(3)     DEFAULT NULL COMMENT '最后成功时间',
    last_error_msg  VARCHAR(500)    DEFAULT NULL COMMENT '最后错误信息',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_provider_id (provider_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商健康状态';
