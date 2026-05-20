-- RAG知识库相关表结构

-- 知识库文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
    `total_chars` INT DEFAULT NULL COMMENT '总字符数',
    `chunk_count` INT DEFAULT NULL COMMENT '分块数量',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-处理中, 1-已完成, 2-失败',
    `error_msg` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- 文档分块表
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分块ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
    `content` TEXT NOT NULL COMMENT '分块内容',
    `chunk_index` INT NOT NULL COMMENT '分块序号',
    `char_count` INT DEFAULT NULL COMMENT '字符数',
    `start_pos` INT DEFAULT NULL COMMENT '起始位置',
    `end_pos` INT DEFAULT NULL COMMENT '结束位置',
    `vector_id` VARCHAR(64) DEFAULT NULL COMMENT 'Milvus向量ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_vector_id` (`vector_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分块表';
