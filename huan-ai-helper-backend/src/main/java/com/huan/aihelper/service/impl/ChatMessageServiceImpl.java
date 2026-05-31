package com.huan.aihelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huan.aihelper.mapper.ChatMessageMapper;
import com.huan.aihelper.model.entity.ChatMessage;
import com.huan.aihelper.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Override
    public int getMaxSeqNo(Long conversationId) {
        return baseMapper.selectMaxSeqNo(conversationId);
    }

    @Override
    public List<ChatMessage> getInContextMessages(Long conversationId) {
        return baseMapper.selectInContext(conversationId);
    }

    @Override
    public void saveWithSeqNo(ChatMessage chatMessage) {
        int maxSeqNo = getMaxSeqNo(chatMessage.getConversationId());
        chatMessage.setSeqNo(maxSeqNo + 1);
        save(chatMessage);
    }

    @Override
    public List<ChatMessage> listByConversationId(Long conversationId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getSeqNo);
        return list(wrapper);
    }

    @Override
    public List<ChatMessage> listByConversationIdBeforeId(Long conversationId, Long beforeId, int limit) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId);
        if (beforeId != null) {
            wrapper.lt(ChatMessage::getId, beforeId);
        }
        wrapper.orderByDesc(ChatMessage::getId)
                .last("LIMIT " + limit);
        List<ChatMessage> result = list(wrapper);
        result.sort((a, b) -> Integer.compare(a.getSeqNo(), b.getSeqNo()));
        return result;
    }

    @Override
    public void clearByConversationId(Long conversationId) {
        LambdaUpdateWrapper<ChatMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId)
                .set(ChatMessage::getIsDeleted, 1);
        update(wrapper);
    }
}
