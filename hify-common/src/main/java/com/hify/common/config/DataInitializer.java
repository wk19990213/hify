package com.hify.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时自动建表（兜底 spring.sql.init 可能不生效的情况）。
 */
@Slf4j
@Component
public class DataInitializer {

    private final DataSource dataSource;

    public DataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initUserTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS hify_user (
                    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
                    username      VARCHAR(64)   NOT NULL COMMENT '用户名',
                    password_hash VARCHAR(255)  NOT NULL COMMENT '密码哈希（BCrypt）',
                    display_name  VARCHAR(64)   DEFAULT NULL COMMENT '显示名称',
                    status        TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
                    created_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
                    updated_at    DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
                    deleted       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表'
            """);
            log.info("hify_user table ensured");
        } catch (Exception e) {
            log.error("Failed to create hify_user table: {}", e.getMessage());
        }
    }
}
