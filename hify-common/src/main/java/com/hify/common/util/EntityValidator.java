package com.hify.common.util;

import com.hify.common.exception.BizException;

/**
 * 实体存在性验证工具 — 统一 ServiceImpl 中的非空检查模式。
 * 注意：软删除检查由 MyBatis-Plus @TableLogic 自动处理，此处仅验证非空。
 */
public final class EntityValidator {

    private EntityValidator() {
    }

    /**
     * 验证实体非空，否则抛出数据不存在异常
     */
    public static <T> T requireNonNull(T entity, String message) {
        if (entity == null) {
            throw BizException.notFound(message);
        }
        return entity;
    }
}
