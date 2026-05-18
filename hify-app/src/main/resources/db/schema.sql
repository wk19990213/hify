-- ============================================
-- Hify 建表 DDL
-- 规范：主键 id BIGINT 自增、
--       时间字段 created_at/updated_at DATETIME(3)、
--       逻辑删除 deleted TINYINT(1) DEFAULT 0、
--       字符集 utf8mb4、引擎 InnoDB
-- ============================================

-- ----------------------------
-- 1. 模型提供商
-- ----------------------------
CREATE TABLE IF NOT EXISTS provider (
    id           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    name         VARCHAR(64)     NOT NULL COMMENT '提供商名称',
    code         VARCHAR(32)     NOT NULL COMMENT '提供商编码（openai/claude/gemini/ollama）',
    api_base_url VARCHAR(255)    DEFAULT NULL COMMENT 'API 基础地址',
    api_key      VARCHAR(512)    DEFAULT NULL COMMENT 'API Key（加密存储）',
    description  VARCHAR(255)    DEFAULT '' COMMENT '描述',
    status       TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型提供商';

-- ----------------------------
-- 2. 模型配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS model_config (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    provider_id    BIGINT        NOT NULL COMMENT '提供商 ID',
    model_name     VARCHAR(64)   NOT NULL COMMENT '模型名称（如 gpt-4、claude-3-opus）',
    display_name   VARCHAR(64)   DEFAULT NULL COMMENT '展示名称',
    model_type     VARCHAR(32)   NOT NULL DEFAULT 'llm' COMMENT '模型类型：llm/embedding/rerank',
    context_window INT           DEFAULT 4096 COMMENT '上下文窗口大小（token 数）',
    max_tokens     INT           DEFAULT 2048 COMMENT '最大输出 token 数',
    is_stream      TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否支持流式：0=否，1=是',
    config_json    TEXT          DEFAULT NULL COMMENT '扩展配置（JSON）',
    status         TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
    created_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at     DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    INDEX idx_provider_id (provider_id),
    INDEX idx_model_type (model_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置';

-- ----------------------------
-- 3. Agent 配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS agent (
    id                      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name                    VARCHAR(128)  NOT NULL COMMENT 'Agent 名称',
    description             VARCHAR(512)  DEFAULT '' COMMENT '描述',
    system_prompt           MEDIUMTEXT    DEFAULT NULL COMMENT '系统提示词',
    model_config_id         BIGINT        DEFAULT NULL COMMENT '模型配置 ID',
    conversation_max_rounds INT           DEFAULT 20 COMMENT '最大对话轮数',
    temperature             DECIMAL(3,2)  DEFAULT 0.70 COMMENT '温度参数',
    top_p                   DECIMAL(3,2)  DEFAULT 1.00 COMMENT 'Top-P 采样',
    status                  VARCHAR(16)   NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/archived',
    created_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted                 TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    INDEX idx_model_config_id (model_config_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 配置';

-- ----------------------------
-- 4. Agent 工具绑定
-- ----------------------------
CREATE TABLE IF NOT EXISTS agent_tool (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    agent_id      BIGINT        NOT NULL COMMENT 'Agent ID',
    tool_name     VARCHAR(64)   NOT NULL COMMENT '工具名称',
    tool_type     VARCHAR(16)   NOT NULL DEFAULT 'builtin' COMMENT '工具类型：mcp/builtin',
    mcp_server_id BIGINT        DEFAULT NULL COMMENT 'MCP 服务 ID（tool_type=mcp 时）',
    config_json   TEXT          DEFAULT NULL COMMENT '工具配置（JSON）',
    created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 工具绑定';

-- ----------------------------
-- 5. MCP 服务器配置
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
-- 6. 对话会话
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
-- 7. 对话消息
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
