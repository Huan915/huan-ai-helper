package com.huan.aihelper.controller;

import com.huan.aihelper.common.FileContentCache;
import com.huan.aihelper.common.Result;
import com.huan.aihelper.manager.OssManager;
import com.huan.aihelper.service.FileParseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/file")
public class FileUploadController {

    @Resource
    private OssManager ossManager;

    @Resource
    private FileContentCache fileContentCache;

    @Resource
    private FileParseService fileParseService;

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "txt", "md");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Operation(summary = "上传文件并解析")
    @PostMapping("/upload")
    public Result<FileUploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Result.error("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_TYPES.contains(extension)) {
            return Result.error("仅支持 PDF、TXT、MD 格式文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("文件大小不能超过10MB");
        }

        try {
            String parsedContent = fileParseService.parseBytes(file.getBytes(), extension);

            String objectName = "chat-attachment/" + UUID.randomUUID() + "/" + originalFilename;
            ossManager.upload(file, objectName);
            String fileUrl = ossManager.getPublicUrl(objectName);

            FileUploadResult result = new FileUploadResult();
            result.setName(originalFilename);
            result.setType(extension);
            result.setUrl(fileUrl);
            result.setSize(file.getSize());
            result.setParsedContent(parsedContent);

            if (parsedContent != null && !parsedContent.isEmpty()) {
                fileContentCache.put(fileUrl, parsedContent);
                fileContentCache.cleanup();
            }

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("文件解析失败: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return filename.substring(dotIndex + 1);
    }

    @lombok.Data
    public static class FileUploadResult {
        private String name;
        private String type;
        private String url;
        private Long size;
        private String parsedContent;
    }
}
