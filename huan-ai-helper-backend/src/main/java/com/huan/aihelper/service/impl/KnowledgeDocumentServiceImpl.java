package com.huan.aihelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huan.aihelper.mapper.KnowledgeDocumentMapper;
import com.huan.aihelper.model.entity.KnowledgeDocument;
import com.huan.aihelper.service.KnowledgeDocumentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    @Override
    public List<KnowledgeDocument> listByKbId(String kbId) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getKbId, kbId)
                .orderByDesc(KnowledgeDocument::getCreatedAt);
        return list(wrapper);
    }
}