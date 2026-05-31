package com.huan.aihelper.controller;

import com.huan.aihelper.app.ChatApp;
import com.huan.aihelper.common.Result;
import com.huan.aihelper.model.entity.ChatConversation;
import com.huan.aihelper.model.entity.ChatMessage;
import com.huan.aihelper.model.request.conversation.ChatConversationCreateRequest;
import com.huan.aihelper.model.request.conversation.ChatConversationRenameRequest;
import com.huan.aihelper.model.request.conversation.ChatMessageSendRequest;
import com.huan.aihelper.service.ChatConversationService;
import com.huan.aihelper.service.ChatMessageService;
import com.huan.aihelper.tools.CurrentTimeTool;
import com.huan.aihelper.tools.WebSearchTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "会话管理")
@Slf4j
@RestController
@RequestMapping("/conversation")
public class ChatConversationController {

    @Resource
    private ChatConversationService chatConversationService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatApp chatApp;

    @Operation(summary = "创建会话")
    @PostMapping("/create")
    public Result<ChatConversation> createConversation(@RequestBody ChatConversationCreateRequest request) {
        try {
            ChatConversation conversation = chatConversationService.createConversation(request);
            return Result.success(conversation);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取会话详情")
    @GetMapping("/{id}")
    public Result<ChatConversation> getConversation(@PathVariable Long id) {
        try {
            ChatConversation conversation = chatConversationService.getById(id);
            if (conversation == null) {
                return Result.error("会话不存在");
            }
            return Result.success(conversation);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取用户会话列表")
    @GetMapping("/list")
    public Result<List<ChatConversation>> listConversations(@RequestParam Long userId) {
        try {
            List<ChatConversation> conversations = chatConversationService.listConversations(userId);
            return Result.success(conversations);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteConversation(@PathVariable Long id) {
        try {
            boolean success = chatConversationService.removeById(id);
            return Result.success(success);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "重命名会话")
    @PutMapping("/{id}/rename")
    public Result<ChatConversation> renameConversation(@PathVariable Long id, @RequestBody ChatConversationRenameRequest request) {
        try {
            ChatConversation conversation = chatConversationService.getById(id);
            if (conversation == null) {
                return Result.error("会话不存在");
            }
            conversation.setTitle(request.getTitle());
            chatConversationService.updateById(conversation);
            return Result.success(conversation);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Value("${baidu.ai-search.api-key}")
    private String apiKey;

    @Operation(summary = "发送消息")
    @PostMapping(value = "/{id}/message",  produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable Long id, @RequestBody ChatMessageSendRequest request) {
        String content = request != null ? request.getContent() : null;
        Boolean gain = request != null ? request.getGain() : null;
        Boolean deepThinking = request != null ? request.getDeepThinking() : null;
        var attachments = request != null ? request.getAttachments() : null;

        if (attachments != null && !attachments.isEmpty()) {
            for (var att : attachments) {
                log.info("附件: name={}, type={}, parsedContent长度={}", att.getName(), att.getType(),
                        att.getParsedContent() != null ? att.getParsedContent().length() : "null");
            }
        } else {
            log.info("无附件");
        }

        final ToolCallback[] toolCallbacks;
        if (Boolean.TRUE.equals(gain)) {
            toolCallbacks = ToolCallbacks.from(new CurrentTimeTool(), new WebSearchTool(apiKey));
        } else {
            toolCallbacks = ToolCallbacks.from(new CurrentTimeTool());
        }

        if (Boolean.TRUE.equals(deepThinking)) {
            return chatApp.doChatStreamDeepThinking(id, content, toolCallbacks, attachments);
        }
        return chatApp.doChatStream(id, content, toolCallbacks, attachments);
    }

    @Operation(summary = "获取会话消息列表")
    @GetMapping("/{id}/messages")
    public Result<List<ChatMessage>> listMessages(@PathVariable Long id) {
        try {
            List<ChatMessage> messages = chatMessageService.listByConversationId(id);
            return Result.success(messages);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "分页获取会话消息")
    @GetMapping("/{id}/messages/page")
    public Result<List<ChatMessage>> listMessagesPage(
            @PathVariable Long id,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<ChatMessage> messages = chatMessageService.listByConversationIdBeforeId(id, beforeId, limit);
            return Result.success(messages);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
