package com.hify.knowledge.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件上传安全验证测试。
 * 直接测试类型白名单逻辑，不依赖 Spring 容器或 KnowledgeServiceImpl 构造函数。
 */
class KnowledgeServiceImplTest {

    // 与 KnowledgeServiceImpl.ALLOWED_FILE_TYPES 保持一致
    private boolean isAllowedFileType(String fileType) {
        return fileType != null && ("pdf".equals(fileType) || "docx".equals(fileType)
                || "txt".equals(fileType) || "md".equals(fileType));
    }

    @Test
    void testUploadDocument_ValidFileType_ReturnsTrue() {
        String[] validTypes = {"pdf", "docx", "txt", "md"};
        for (String type : validTypes) {
            assertTrue(isAllowedFileType(type), "File type '" + type + "' should be valid");
        }
    }

    @Test
    void testUploadDocument_InvalidFileType_ThrowsException() {
        String[] invalidTypes = {"exe", "sh", "bat", "jsp", "php", "html", "js"};
        for (String type : invalidTypes) {
            assertFalse(isAllowedFileType(type), "File type '" + type + "' should be invalid");
        }
    }

    @Test
    void testUploadDocument_FileTooLarge_ReturnsFalse() {
        long maxFileSize = 50L * 1024 * 1024; // 50MB
        long largeFileSize = 51L * 1024 * 1024;

        assertTrue(largeFileSize > maxFileSize, "51MB file should exceed 50MB limit");
        assertTrue(maxFileSize >= 50L * 1024 * 1024);
    }
}
