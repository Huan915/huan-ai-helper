package com.huan.aihelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huan.aihelper.model.entity.KnowledgeDocument;

import java.util.List;

public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    List<KnowledgeDocument> listByKbId(String kbId);
}