package com.huan.aihelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huan.aihelper.model.entity.ChatConversation;
import com.huan.aihelper.model.request.conversation.ChatConversationCreateRequest;

import java.util.List;

public interface ChatConversationService extends IService<ChatConversation> {

    ChatConversation createConversation(ChatConversationCreateRequest request);

    List<ChatConversation> listConversations(Long userId);

    void updateStats(Long conversationId);
}
