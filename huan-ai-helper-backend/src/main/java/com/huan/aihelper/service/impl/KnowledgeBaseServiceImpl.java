package com.huan.aihelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huan.aihelper.mapper.KnowledgeBaseMapper;
import com.huan.aihelper.model.entity.KnowledgeBase;
import com.huan.aihelper.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Override
    public KnowledgeBase getByKbId(String kbId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getKbId, kbId);
        return getOne(wrapper);
    }

    @Override
    public List<KnowledgeBase> listActive() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getStatus, 0)
                .orderByDesc(KnowledgeBase::getCreatedAt);
        return list(wrapper);
    }
}