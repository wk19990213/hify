package com.hify.knowledge.service.impl;

import com.hify.knowledge.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    @Override
    public String parseDocument(MultipartFile file, String fileType) {
        try (InputStream in = file.getInputStream()) {
            return switch (fileType) {
                case "pdf" -> parsePdf(in);
                case "docx" -> parseDocx(in);
                default -> new String(in.readAllBytes());
            };
        } catch (Exception e) {
            log.error("Document parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String parsePdf(InputStream in) throws Exception {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    private String parseDocx(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }
}
