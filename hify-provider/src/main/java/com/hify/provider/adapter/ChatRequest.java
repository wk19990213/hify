package com.hify.provider.adapter;

import java.util.List;
import java.util.Map;

/** 统一聊天请求，适配器内部使用 */
public record ChatRequest(
        String model,
        List<Map<String, String>> messages,
        double temperature,
        boolean stream
) {}
