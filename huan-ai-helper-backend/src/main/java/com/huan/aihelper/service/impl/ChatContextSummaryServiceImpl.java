package com.huan.aihelper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huan.aihelper.mapper.ChatContextSummaryMapper;
import com.huan.aihelper.model.entity.ChatContextSummary;
import com.huan.aihelper.service.ChatContextSummaryService;
import org.springframework.stereotype.Service;

@Service
public class ChatContextSummaryServiceImpl extends ServiceImpl<ChatContextSummaryMapper, ChatContextSummary> implements ChatContextSummaryService {

    @Override
    public ChatContextSummary getLatestSummary(Long conversationId) {
        return baseMapper.selectLatest(conversationId);
    }
}
