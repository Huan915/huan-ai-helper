package com.huan.aihelper.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class LoveAppDocumentLoaderTest {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Test
    void loadDocuments() {
        List<Document> documents = loveAppDocumentLoader.loadDocuments();
        for (Document document : documents) {
            System.out.println(document);
        }
        System.out.println(documents.size());

    }
}