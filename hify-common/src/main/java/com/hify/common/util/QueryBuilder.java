package com.hify.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

/**
 * 通用查询条件构建器 - 标准化 LambdaQueryWrapper 常见模式。
 * 注意：逻辑删除过滤（deleted=0）由 MyBatis-Plus @TableLogic 自动处理。
 */
public final class QueryBuilder {

    private QueryBuilder() {
    }

    /** 创建基础查询 */
    public static <T> LambdaQueryWrapper<T> create() {
        return new LambdaQueryWrapper<>();
    }

    /** 等值查询快捷方法 */
    public static <T> LambdaQueryWrapper<T> eq(SFunction<T, ?> column, Object value) {
        return new LambdaQueryWrapper<T>().eq(column, value);
    }

    /** 创建带排序的查询 */
    public static <T> LambdaQueryWrapper<T> orderByDesc(Class<T> clazz,
                                                         SFunction<T, ?> sortColumn) {
        return new LambdaQueryWrapper<T>().orderByDesc(sortColumn);
    }

    /** 创建带 IN 条件的查询 */
    public static <T> LambdaQueryWrapper<T> in(SFunction<T, ?> column,
                                                java.util.Collection<?> values) {
        return new LambdaQueryWrapper<T>().in(column, values);
    }
}
