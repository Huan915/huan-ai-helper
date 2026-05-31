package com.huan.aihelper.config;

import com.huan.aihelper.rag.LoveAppDocumentLoader;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class PgVectorVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024)                    // Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10)         // Optional: defaults to 10000
                .build();
//        initDocumentsIfNeeded(vectorStore);
        return vectorStore;
    }

    private void initDocumentsIfNeeded(PgVectorStore vectorStore) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query("test")
                .topK(1)
                .build();

        var results = vectorStore.similaritySearch(searchRequest);

        if (results.isEmpty()) {
            System.out.println("首次初始化，加载文档到 PgVector...");
            List<Document> documents = loveAppDocumentLoader.loadDocuments();

            int batchSize = 10;
            int totalDocuments = documents.size();
            for (int i = 0; i < totalDocuments; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalDocuments);
                List<Document> batch = documents.subList(i, endIndex);
                vectorStore.add(batch);
                System.out.println("已加载批次 " + (i / batchSize + 1) + ": " + batch.size() + " 个文档");
            }
            System.out.println("成功加载 " + totalDocuments + " 个文档");
        } else {
            System.out.println("PgVector 已存在数据，跳过初始化");
        }
    }
}
