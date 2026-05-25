package com.hify.common.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * AES-256-GCM 加解密工具。
 * 密钥从环境变量 HIFY_ENCRYPTION_KEY 读取（64 位 hex = 32 字节）。
 * 若未设置则降级为明文直传（开发环境）。
 */
@Slf4j
public final class AesEncryptor {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static final SecretKey MASTER_KEY;
    private static final boolean ENABLED;

    static {
        String keyHex = System.getenv("HIFY_ENCRYPTION_KEY");
        SecretKey key = null;
        boolean enabled = false;
        if (keyHex != null && keyHex.length() == 64) {
            try {
                byte[] keyBytes = HexFormat.of().parseHex(keyHex);
                key = new SecretKeySpec(keyBytes, "AES");
                enabled = true;
                log.info("AesEncryptor initialized with HIFY_ENCRYPTION_KEY");
            } catch (IllegalArgumentException e) {
                log.error("HIFY_ENCRYPTION_KEY is not valid hex. AesEncryptor disabled.", e);
            }
        } else {
            log.warn("HIFY_ENCRYPTION_KEY not set or invalid length (need 64 hex chars). AesEncryptor disabled, plaintext mode.");
        }
        MASTER_KEY = key;
        ENABLED = enabled;
    }

    private AesEncryptor() {
    }

    public static String encrypt(String plaintext) {
        if (!ENABLED || plaintext == null) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, MASTER_KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("AES encrypt failed", e);
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    public static String decrypt(String ciphertext) {
        if (!ENABLED || ciphertext == null) {
            return ciphertext;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(ciphertext));
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, MASTER_KEY, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES decrypt failed", e);
            throw new RuntimeException("AES decryption failed", e);
        }
    }
}
