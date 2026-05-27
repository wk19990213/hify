package com.hify.knowledge.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentParserService {

    /**
     * 解析上传的文档文件，提取文本内容。
     *
     * @param file     上传的文件
     * @param fileType 文件类型（pdf/docx/txt/md）
     * @return 提取的文本内容，解析失败时返回 null
     */
    String parseDocument(MultipartFile file, String fileType);
}
