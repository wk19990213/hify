package com.hify.knowledge.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传安全验证服务。
 * 防护措施：文件大小限制、类型白名单、路径穿越检查、魔数验证。
 */
public interface FileValidationService {

    /**
     * 验证上传文件的安全性。
     * 防护措施完备：文件大小限制(50MB)、类型白名单(pdf/docx/txt/md)、
     * 路径穿越检查(..\\/)、魔数验证(防伪造扩展名)。
     * 新增文件类型时需同步更新 ALLOWED_FILE_TYPES 和魔数验证逻辑。
     */
    void validate(MultipartFile file);
}
