package com.huan.aihelper.chatmemory;

import com.huan.aihelper.model.entity.ChatConversation;
import com.huan.aihelper.model.entity.ChatMessage;
import com.huan.aihelper.service.ChatConversationService;
import com.huan.aihelper.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DatabaseChatMemory implements ChatMemory {

    private final ChatMessageService chatMessageService;
    private final ChatConversationService chatConversationService;

    public DatabaseChatMemory(ChatMessageService chatMessageService,
                              ChatConversationService chatConversationService) {
        this.chatMessageService = chatMessageService;
        this.chatConversationService = chatConversationService;
    }

    @Override
    public void add(@NotNull String conversationId, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }

        Long convId = Long.valueOf(conversationId);

        ChatConversation conversation = chatConversationService.getById(convId);
        if (conversation == null) {
            log.warn("会话不存在: {}", conversationId);
            return;
        }

        int contextWindowSize = conversation.getContextWindowSize() != null
                ? conversation.getContextWindowSize() : 20;

        for (Message message : messages) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setConversationId(convId);
            chatMessage.setRole(message.getMessageType().getValue());
            chatMessage.setContent(message.getText());
            chatMessage.setModelName(conversation.getModelName());
            chatMessage.setContextIncluded(true);
            chatMessageService.saveWithSeqNo(chatMessage);
        }

        slideContextWindow(convId, contextWindowSize);

        chatConversationService.updateStats(convId);
    }

    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {

        Long convId = Long.valueOf(conversationId);
        List<ChatMessage> contextMessages = chatMessageService.getInContextMessages(convId);

        List<Message> result = new ArrayList<>();
        for (ChatMessage chatMessage : contextMessages) {
            Message message = convertToSpringMessage(chatMessage);
            if (message != null) {
                result.add(message);
            }
        }

        return result;
    }

    @Override
    public void clear(@NotNull String conversationId) {

        Long convId = Long.valueOf(conversationId);
        chatMessageService.clearByConversationId(convId);
        log.info("已清除会话 {} 的所有消息", conversationId);
    }

    private void slideContextWindow(Long conversationId, int contextWindowSize) {
        List<ChatMessage> contextMessages = chatMessageService.getInContextMessages(conversationId);

        if (contextMessages.size() <= contextWindowSize) {
            return;
        }

        int excludeCount = contextMessages.size() - contextWindowSize;
        for (int i = 0; i < excludeCount; i++) {
            ChatMessage msg = contextMessages.get(i);
            msg.setContextIncluded(false);
            chatMessageService.updateById(msg);
        }

        log.debug("会话 {} 上下文窗口滑动，移出 {} 条消息", conversationId, excludeCount);
    }

    private Message convertToSpringMessage(ChatMessage chatMessage) {
        String role = chatMessage.getRole();
        String content = chatMessage.getContent();

        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> {
                log.warn("未知的消息角色: {}", role);
                yield null;
            }
        };
    }
}
