package com.huan.aihelper.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagQueryService {

    @Resource
    private VectorStore pgVectorStore;

    public List<Document> searchByKnowledgeBase(String kbId, String query, int topK) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(builder.eq("kb_id", kbId).build())
                .build();

        return pgVectorStore.similaritySearch(request);
    }

    public String buildRagContext(String kbId, String query, int topK) {
        List<Document> relevantDocs = searchByKnowledgeBase(kbId, query, topK);

        if (relevantDocs.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是相关的知识库内容，请参考回答：\n\n");

        for (int i = 0; i < relevantDocs.size(); i++) {
            context.append("【参考资料 ").append(i + 1).append("】\n");
            context.append(relevantDocs.get(i).getText()).append("\n\n");
        }

        context.append("请基于以上参考资料回答用户的问题。如果参考资料不足以回答问题，请如实说明。\n");

        return context.toString();
    }
}