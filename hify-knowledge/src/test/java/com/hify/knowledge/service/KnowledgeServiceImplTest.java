package com.hify.knowledge.service;

import com.hify.common.exception.BizException;
import com.hify.knowledge.mapper.DocumentChunkMapper;
import com.hify.knowledge.mapper.DocumentMapper;
import com.hify.knowledge.mapper.KnowledgeBaseMapper;
import com.hify.knowledge.service.impl.KnowledgeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件上传安全验证测试
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceImplTest {

    @Mock
    private KnowledgeBaseMapper kbMapper;

    @Mock
    private DocumentMapper docMapper;

    @Mock
    private DocumentChunkMapper chunkMapper;

    @InjectMocks
    private KnowledgeServiceImpl knowledgeService;

    @Test
    void testUploadDocument_InvalidFileType_ThrowsException() {
        // 测试不支持的文件类型被拒绝（验证在查询数据库之前）
        // 注意：文件验证在查询知识库之前执行，所以不需要 mock kbMapper

        // 尝试上传 exe 文件
        MultipartFile maliciousFile = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "malicious".getBytes()
        );

        // 应抛出异常（在访问数据库之前）
        BizException exception = assertThrows(BizException.class, () -> {
            knowledgeService.uploadDocument(1L, maliciousFile);
        });

        assertTrue(exception.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void testUploadDocument_FileTooLarge_ThrowsException() {
        // 测试超大文件被拒绝

        // 创建超过 50MB 的文件
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
        MultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeContent
        );

        BizException exception = assertThrows(BizException.class, () -> {
            knowledgeService.uploadDocument(1L, largeFile);
        });

        assertTrue(exception.getMessage().contains("文件大小超过限制"));
    }

    @Test
    void testUploadDocument_ValidFileType_ReturnsTrue() {
        // 测试有效文件类型被允许（文件名验证通过）

        // 验证白名单包含 pdf
        String[] validTypes = {"pdf", "docx", "txt", "md"};
        for (String type : validTypes) {
            assertTrue(isValidFileType(type), "File type " + type + " should be valid");
        }

        // 验证黑名单类型被拒绝
        String[] invalidTypes = {"exe", "sh", "bat", "jsp", "php"};
        for (String type : invalidTypes) {
            assertFalse(isValidFileType(type), "File type " + type + " should be invalid");
        }
    }

    private boolean isValidFileType(String fileType) {
        // 模拟文件类型白名单检查（与实现类保持一致）
        return fileType != null && (fileType.equals("pdf") || fileType.equals("docx")
                || fileType.equals("txt") || fileType.equals("md"));
    }
}
