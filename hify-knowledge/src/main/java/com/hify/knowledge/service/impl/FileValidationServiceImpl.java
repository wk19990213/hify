package com.hify.knowledge.service.impl;

import com.hify.common.exception.BizException;
import com.hify.knowledge.service.FileValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;

@Slf4j
@Service
public class FileValidationServiceImpl implements FileValidationService {

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("pdf", "docx", "txt", "md");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Override
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.paramError("文件不能为空");
        }

        // 1. 文件大小限制
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.paramError("文件大小超过限制（最大50MB）");
        }

        // 2. 文件类型白名单验证
        String fileName = file.getOriginalFilename();
        String fileType = fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";

        if (!ALLOWED_FILE_TYPES.contains(fileType)) {
            throw BizException.paramError("不支持的文件类型，仅支持：pdf, docx, txt, md");
        }

        // 3. 文件名安全检查：禁止路径遍历
        if (fileName != null && (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\"))) {
            throw BizException.paramError("文件名包含非法字符");
        }

        // 4. 魔数检查（针对PDF和DOCX）
        validateFileMagicNumber(file, fileType);
    }

    /**
     * 验证文件魔数，防止伪造扩展名
     */
    void validateFileMagicNumber(MultipartFile file, String fileType) {
        try (InputStream is = file.getInputStream()) {
            byte[] magic = new byte[4];
            int read = is.read(magic);
            if (read < 4) return; // 文件太小，无法检查

            // PDF: %PDF (0x25 0x50 0x44 0x46)
            if ("pdf".equals(fileType)) {
                if (magic[0] != 0x25 || magic[1] != 0x50 || magic[2] != 0x44 || magic[3] != 0x46) {
                    throw BizException.paramError("文件内容不是有效的PDF格式");
                }
            }
            // DOCX: PK (ZIP格式，0x50 0x4B)
            else if ("docx".equals(fileType)) {
                if (magic[0] != 0x50 || magic[1] != 0x4B) {
                    throw BizException.paramError("文件内容不是有效的DOCX格式");
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件魔数检查失败", e);
            throw BizException.paramError("文件验证失败");
        }
    }
}
