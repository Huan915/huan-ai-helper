package com.huan.aihelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huan.aihelper.mapper.ChatConversationMapper;
import com.huan.aihelper.mapper.ChatMessageMapper;
import com.huan.aihelper.model.entity.ChatConversation;
import com.huan.aihelper.model.entity.ChatMessage;
import com.huan.aihelper.model.request.conversation.ChatConversationCreateRequest;
import com.huan.aihelper.service.ChatConversationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation> implements ChatConversationService {

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public ChatConversation createConversation(ChatConversationCreateRequest request) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(request.getUserId());
        conversation.setTitle(request.getTitle() != null ? request.getTitle() : "新会话");
        conversation.setSystemPrompt(request.getSystemPrompt());
        conversation.setConversationType(request.getConversationType() != null ? request.getConversationType() : "NORMAL");
        conversation.setModelName(request.getModelName() != null ? request.getModelName() : "qwen-plus");
        conversation.setContextWindowSize(request.getContextWindowSize() != null ? request.getContextWindowSize() : 20);
        conversation.setContextStrategy(request.getContextStrategy() != null ? request.getContextStrategy() : "SLIDING_WINDOW");
        conversation.setTemperature(request.getTemperature() != null ? request.getTemperature() : new BigDecimal("0.7"));
        conversation.setTopP(request.getTopP() != null ? request.getTopP() : new BigDecimal("0.8"));
        conversation.setStatus(1);
        conversation.setTotalTokens(0L);
        conversation.setTotalMessages(0);
        conversation.setLastMessageTime(null);
        save(conversation);
        log.info("创建会话成功, id={}, userId={}, type={}", conversation.getId(), conversation.getUserId(), conversation.getConversationType());
        return conversation;
    }

    @Override
    public List<ChatConversation> listConversations(Long userId) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .orderByDesc(ChatConversation::getLastMessageTime)
                .orderByDesc(ChatConversation::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public void updateStats(Long conversationId) {
        ChatConversation conversation = getById(conversationId);
        if (conversation == null) {
            return;
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId);
        Long messageCount = chatMessageMapper.selectCount(wrapper);

        conversation.setTotalMessages(messageCount.intValue());
        conversation.setLastMessageTime(LocalDateTime.now());
        updateById(conversation);
    }
}
