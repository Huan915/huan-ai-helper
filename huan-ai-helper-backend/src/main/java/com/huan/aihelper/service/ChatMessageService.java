package com.huan.aihelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huan.aihelper.model.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {

    int getMaxSeqNo(Long conversationId);

    List<ChatMessage> getInContextMessages(Long conversationId);

    void saveWithSeqNo(ChatMessage chatMessage);

    List<ChatMessage> listByConversationId(Long conversationId);

    List<ChatMessage> listByConversationIdBeforeId(Long conversationId, Long beforeId, int limit);

    void clearByConversationId(Long conversationId);
}
