package com.hify.common.util;

import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实体转换工具 — 减少 ServiceImpl 中重复的 BeanUtils.copyProperties 模式。
 */
public final class EntityConverter {

    private EntityConverter() {
    }

    /**
     * 将源对象属性复制到目标类型的新实例
     */
    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("实体转换失败: " + targetClass.getSimpleName(), e);
        }
    }

    /**
     * 批量转换实体列表
     */
    public static <T> List<T> convertList(List<?> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetClass))
                .collect(Collectors.toList());
    }
}
