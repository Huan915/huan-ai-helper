package com.huan.aihelper.manager;

import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.OSSObjectSummary;
import com.huan.aihelper.config.OssConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class OssManager {

    @Resource
    private OSS ossClient;

    @Resource
    private OssConfig ossConfig;

    public String upload(MultipartFile file) {
        return upload(file, null);
    }

    public String upload(MultipartFile file, String objectName) {
        try {
            if (objectName == null || objectName.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                objectName = IdUtil.simpleUUID() + extension;
            }

            ossClient.putObject(ossConfig.getBucketName(), objectName, file.getInputStream());
            log.info("文件上传成功: bucket={}, objectName={}", ossConfig.getBucketName(), objectName);
            return objectName;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    public String upload(byte[] data, String objectName) {
        ossClient.putObject(ossConfig.getBucketName(), objectName, new ByteArrayInputStream(data));
        log.info("字节数组上传成功: bucket={}, objectName={}", ossConfig.getBucketName(), objectName);
        return objectName;
    }

    public String upload(InputStream inputStream, String objectName) {
        try {
            ossClient.putObject(ossConfig.getBucketName(), objectName, inputStream);
            log.info("流上传成功: bucket={}, objectName={}", ossConfig.getBucketName(), objectName);
            return objectName;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("关闭输入流失败", e);
            }
        }
    }

    public InputStream download(String objectName) {
        OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), objectName);
        return ossObject.getObjectContent();
    }

    public byte[] downloadBytes(String objectName) {
        try (OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), objectName);
             InputStream inputStream = ossObject.getObjectContent()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("文件下载失败: objectName={}", objectName, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    public void delete(String objectName) {
        ossClient.deleteObject(ossConfig.getBucketName(), objectName);
        log.info("文件删除成功: bucket={}, objectName={}", ossConfig.getBucketName(), objectName);
    }

    public void deleteBatch(List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return;
        }
        for (String objectName : objectNames) {
            ossClient.deleteObject(ossConfig.getBucketName(), objectName);
        }
        log.info("批量删除成功: count={}", objectNames.size());
    }

    public boolean exists(String objectName) {
        return ossClient.doesObjectExist(ossConfig.getBucketName(), objectName);
    }

    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(objectName, 3600);
    }

    public String getPresignedUrl(String objectName, long expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
        URL url = ossClient.generatePresignedUrl(ossConfig.getBucketName(), objectName, expiration);
        return url.toString();
    }

    public String getPublicUrl(String objectName) {
        return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectName;
    }

    public List<String> listObjects() {
        return listObjects(null);
    }

    public List<String> listObjects(String prefix) {
        List<String> result = new ArrayList<>();
        ObjectListing objectListing = ossClient.listObjects(ossConfig.getBucketName(), prefix);
        for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
            result.add(summary.getKey());
        }
        return result;
    }

    public void copy(String sourceObjectName, String destObjectName) {
        ossClient.copyObject(ossConfig.getBucketName(), sourceObjectName, ossConfig.getBucketName(), destObjectName);
        log.info("文件复制成功: {} -> {}", sourceObjectName, destObjectName);
    }

    public long getFileSize(String objectName) {
        OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), objectName);
        return ossObject.getObjectMetadata().getContentLength();
    }
}