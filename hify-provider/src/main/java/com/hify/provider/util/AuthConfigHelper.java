package com.hify.provider.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.crypto.AesEncryptor;

import java.util.Collections;
import java.util.Map;

/**
 * Provider authConfig 解密工具类。
 * 集中处理 AesEncryptor.decrypt() + ObjectMapper.readValue() 模式，
 * 消除跨模块重复代码。
 */
public class AuthConfigHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 解密 authConfig 并返回 Map。
     * @param encrypted 加密的 authConfig JSON 字符串
     * @return 解密后的 Map，失败返回空 Map
     */
    public static Map<String, Object> decryptAuthConfig(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            String json = AesEncryptor.decrypt(encrypted);
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 从 authConfig 中提取 apiKey。
     */
    public static String extractApiKey(String encrypted) {
        Map<String, Object> config = decryptAuthConfig(encrypted);
        Object key = config.get("apiKey");
        return key != null ? key.toString() : null;
    }
}
