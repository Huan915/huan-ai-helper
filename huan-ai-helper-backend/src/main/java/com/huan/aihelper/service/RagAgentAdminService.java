package com.huan.aihelper.service;

import cn.hutool.core.util.IdUtil;
import com.huan.aihelper.manager.OssManager;
import com.huan.aihelper.model.entity.AgentConfig;
import com.huan.aihelper.model.entity.KnowledgeBase;
import com.huan.aihelper.model.entity.KnowledgeDocument;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RagAgentAdminService {

    private static final String AVATAR_DIR = "agent-avatar/";

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private AgentConfigService agentConfigService;

    @Resource
    private VectorStore pgVectorStore;

    @Resource
    private OssManager ossManager;

    @Transactional(rollbackFor = Exception.class)
    public AgentConfig createRagAgent(String agentName, String description, String systemPrompt, 
                                       MultipartFile avatar, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new RuntimeException("请至少上传一份文档");
        }

        String kbId = IdUtil.simpleUUID();
        String agentId = IdUtil.simpleUUID();

        KnowledgeBase kb = new KnowledgeBase();
        kb.setKbId(kbId);
        kb.setKbName(agentName);
        kb.setDescription(description);
        kb.setDocumentCount(0);
        kb.setChunkCount(0);
        kb.setStatus(0);
        knowledgeBaseService.save(kb);
        log.info("创建知识库: kbId={}, kbName={}", kbId, agentName);

        int totalChunks = 0;
        int successDocs = 0;

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String fileType = getFileExtension(originalFilename);

            if (!"md".equals(fileType) && !"txt".equals(fileType)) {
                log.warn("跳过不支持的文件类型: {}", originalFilename);
                throw new RuntimeException("不支持的文件类型: " + originalFilename);
            }

            String docId = IdUtil.simpleUUID();
            KnowledgeDocument docRecord = new KnowledgeDocument();
            docRecord.setKbId(kbId);
            docRecord.setDocId(docId);
            docRecord.setFileName(originalFilename);
            docRecord.setFileType(fileType);
            docRecord.setChunkCount(0);
            docRecord.setStatus("PROCESSING");
            knowledgeDocumentService.save(docRecord);

            try {
                List<Document> chunks = parseFile(file, fileType, kbId, docId);
                int batchSize = 10;
                int totalChunksForDoc = chunks.size();
                for (int i = 0; i < totalChunksForDoc; i += batchSize) {
                    int endIndex = Math.min(i + batchSize, totalChunksForDoc);
                    List<Document> batch = chunks.subList(i, endIndex);
                    pgVectorStore.add(batch);
                }
                docRecord.setChunkCount(chunks.size());
                docRecord.setStatus("COMPLETED");
                knowledgeDocumentService.updateById(docRecord);

                totalChunks += chunks.size();
                successDocs++;
                log.info("文档处理完成: {}, 分块数: {}", originalFilename, chunks.size());
            } catch (Exception e) {
                docRecord.setStatus("FAILED");
                knowledgeDocumentService.updateById(docRecord);
                log.error("文档处理失败: {}", originalFilename, e);
                throw new RuntimeException("文档处理失败: " + originalFilename + ", " + e.getMessage());
            }
        }

        kb.setDocumentCount(successDocs);
        kb.setChunkCount(totalChunks);
        knowledgeBaseService.updateById(kb);

        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            avatarUrl = uploadAvatar(avatar, agentId);
        }

        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setAgentId(agentId);
        agentConfig.setAgentName(agentName);
        agentConfig.setDescription(description);
        agentConfig.setAvatarUrl(avatarUrl);
        agentConfig.setSystemPrompt(systemPrompt);
        agentConfig.setKnowledgeBaseId(kbId);
        agentConfig.setMaxSteps(10);
        agentConfig.setStatus(0);
        agentConfigService.save(agentConfig);
        log.info("创建RAG智能体: agentId={}, agentName={}, kbId={}, avatarUrl={}", agentId, agentName, kbId, avatarUrl);

        return agentConfig;
    }

    private String uploadAvatar(MultipartFile avatar, String agentId) {
        try {
            String originalFilename = avatar.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = AVATAR_DIR + agentId + extension;
            ossManager.upload(avatar, objectName);
            String avatarUrl = ossManager.getPublicUrl(objectName);
            log.info("头像上传成功: agentId={}, avatarUrl={}", agentId, avatarUrl);
            return avatarUrl;
        } catch (Exception e) {
            log.error("头像上传失败: agentId={}", agentId, e);
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }

    public String updateAvatar(String agentId, MultipartFile avatar) {
        return uploadAvatar(avatar, agentId);
    }

    private List<Document> parseFile(MultipartFile file, String fileType, String kbId, String docId) throws IOException {
        List<Document> documents = new ArrayList<>();
        String originalFilename = file.getOriginalFilename();

        if ("md".equals(fileType)) {
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .withAdditionalMetadata("kb_id", kbId)
                    .withAdditionalMetadata("doc_id", docId)
                    .withAdditionalMetadata("filename", originalFilename)
                    .build();

            org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
            documents.addAll(reader.read());
        } else if ("txt".equals(fileType)) {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            int chunkSize = 500;
            int overlap = 50;
            List<String> chunks = splitText(content, chunkSize, overlap);

            for (int i = 0; i < chunks.size(); i++) {
                Document doc = new Document(chunks.get(i));
                doc.getMetadata().put("kb_id", kbId);
                doc.getMetadata().put("doc_id", docId);
                doc.getMetadata().put("filename", originalFilename);
                doc.getMetadata().put("chunk_index", i);
                documents.add(doc);
            }
        }

        return documents;
    }

    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start += chunkSize - overlap;
        }
        return chunks;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}