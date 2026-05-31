package com.huan.aihelper.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {
    private final LoveAppDocumentLoader loveAppDocumentLoader;

    public LoveAppVectorStoreConfig(LoveAppDocumentLoader loveAppDocumentLoader) {
        this.loveAppDocumentLoader = loveAppDocumentLoader;
    }

//    @Bean
    public VectorStore loveAppVectorStore(EmbeddingModel dashScopeModelEm, RestClient.Builder builder) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashScopeModelEm).build();
        List<Document> documents = loveAppDocumentLoader.loadDocuments();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }
}
