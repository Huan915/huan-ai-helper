package com.huan.aihelper.service;

import com.huan.aihelper.manager.OssManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class FileParseService {

    @Resource
    private OssManager ossManager;

    public String parseFromOss(String fileUrl, String fileType) {
        try {
            String objectName = extractObjectName(fileUrl);
            if (objectName == null) {
                log.warn("无法从URL提取objectName: {}", fileUrl);
                return null;
            }
            byte[] bytes = ossManager.downloadBytes(objectName);
            return parseBytes(bytes, fileType);
        } catch (Exception e) {
            log.error("从OSS下载并解析文件失败: url={}, type={}", fileUrl, fileType, e);
            return null;
        }
    }

    public String parseBytes(byte[] bytes, String fileType) {
        try {
            return switch (fileType) {
                case "pdf" -> parsePdf(bytes);
                case "txt", "md" -> new String(bytes, "UTF-8");
                default -> null;
            };
        } catch (Exception e) {
            log.error("解析文件内容失败: type={}", fileType, e);
            return null;
        }
    }

    private String parsePdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String extractObjectName(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            int idx = url.indexOf(".com/");
            if (idx < 0) return null;
            return java.net.URLDecoder.decode(url.substring(idx + 5), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }
}
