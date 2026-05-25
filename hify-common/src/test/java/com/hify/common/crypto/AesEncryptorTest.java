package com.hify.common.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AES 加密安全测试 - P2-12
 */
class AesEncryptorTest {

    @Test
    void testEncryptDisabled_ReturnsPlaintext() {
        // 未配置密钥时，加密/解密退回明文（开发环境）
        String plaintext = "test-api-key-12345";
        String encrypted = AesEncryptor.encrypt(plaintext);
        // ENABLED=false 时，直接返回原文
        assertEquals(plaintext, encrypted);
    }

    @Test
    void testDecryptDisabled_ReturnsInput() {
        // 未配置密钥时，解密直接返回输入
        String input = "some-ciphertext";
        String decrypted = AesEncryptor.decrypt(input);
        assertEquals(input, decrypted);
    }

    @Test
    void testEncryptNull_ReturnsNull() {
        // 对 null 加密应返回 null（边界情况）
        assertNull(AesEncryptor.encrypt(null));
    }

    @Test
    void testDecryptNull_ReturnsNull() {
        // 对 null 解密应返回 null（边界情况）
        assertNull(AesEncryptor.decrypt(null));
    }

    @Test
    void testEncryptDecryptRoundtrip() {
        // 测试加密解密往返（仅在 ENABLED 模式下有意义）
        // 密钥未配置时，输入输出相同
        String plaintext = "sk-test-api-key-for-provider";
        String encrypted = AesEncryptor.encrypt(plaintext);
        String decrypted = AesEncryptor.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }
}
