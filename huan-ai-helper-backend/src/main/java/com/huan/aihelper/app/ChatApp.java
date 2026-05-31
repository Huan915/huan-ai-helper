package com.huan.aihelper.app;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.huan.aihelper.common.FileContentCache;
import com.huan.aihelper.advisor.MyLoggerAdvisor;
import com.huan.aihelper.chatmemory.DatabaseChatMemory;
import com.huan.aihelper.model.entity.ChatConversation;
import com.huan.aihelper.model.entity.ChatMessage;
import com.huan.aihelper.model.request.conversation.ChatMessageSendRequest;
import com.huan.aihelper.service.ChatConversationService;
import com.huan.aihelper.service.ChatMessageService;
import com.huan.aihelper.service.FileParseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChatApp {

    private final ChatClient chatClient;

    private final ChatClient deepThinkingChatClient;

    @Resource
    private ChatConversationService chatConversationService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private FileContentCache fileContentCache;

    @Resource
    private FileParseService fileParseService;

    public ChatApp(ChatModel dashScopeChatModel,
                   ChatMessageService chatMessageService,
                   ChatConversationService chatConversationService) {
        ChatMemory chatMemory = new DatabaseChatMemory(chatMessageService, chatConversationService);

        this.chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(
                                chatMemory
                        ).build(),
                        new MyLoggerAdvisor()
                )
                .build();

        this.deepThinkingChatClient = ChatClient.builder(dashScopeChatModel)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withEnableThinking(true)
                        .build())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(
                                chatMemory
                        ).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public SseEmitter doChatStreamDeepThinking(Long conversationId, String message, ToolCallback[] toolCallbacks) {
        return doChatStreamDeepThinking(conversationId, message, toolCallbacks, null);
    }

    public SseEmitter doChatStreamDeepThinking(Long conversationId, String message, ToolCallback[] toolCallbacks, List<ChatMessageSendRequest.AttachmentInfo> attachments) {

        if (ObjUtil.isNull(conversationId) || StrUtil.isBlank(message)) {
            throw new RuntimeException("会话ID或消息不能为空");
        }
        ChatConversation conversation = chatConversationService.getById(conversationId);
        if (ObjectUtil.isEmpty(conversation)) {
            throw new RuntimeException("会话不存在");
        }

        if ("新会话".equals(conversation.getTitle()) && StrUtil.isNotBlank(message)) {
            String newTitle = message.length() <= 20 ? message : message.substring(0, 20) + "...";
            conversation.setTitle(newTitle);
            chatConversationService.updateById(conversation);
        }

        String thinkingSystemPromptBase = """
                你是分析助手。你的任务是分析用户问题，输出思考过程。
                
                ## 严格规则
                1. 只输出分析过程，绝对不要给出最终答案
                2. 分析内容应包括：
                   - 用户问题的核心意图是什么
                   - 需要哪些信息才能回答
                   - 如果需要外部信息，调用工具获取
                   - 推理步骤和关键结论
                3. 对于简单问候（如"你好"），只需简短回应"用户在打招呼"
                4. 思考要简洁，不要啰嗦
                5. 用与用户相同的语言
                
                记住：对于任何问题，你只给出分析过程，不直接输出答案。
                """;

        String attachmentContext = buildAttachmentContext(attachments);
        final String thinkingSystemPrompt = StrUtil.isNotBlank(attachmentContext)
                ? thinkingSystemPromptBase + "\n\n" + attachmentContext
                : thinkingSystemPromptBase;

        SseEmitter emitter = new SseEmitter(300000L);
        StringBuilder thinkingContent = new StringBuilder();

        CompletableFuture.runAsync(() -> {

            var thinkingPromptBuilder = chatClient.prompt()
                    .system(thinkingSystemPrompt)
                    .user(message)
                    .advisors(advisor ->
                            advisor.param(ChatMemory.CONVERSATION_ID, String.valueOf(conversationId)
                            ));
            if (toolCallbacks != null && toolCallbacks.length > 0) {
                thinkingPromptBuilder = thinkingPromptBuilder.toolCallbacks(toolCallbacks);
            }
            thinkingPromptBuilder.stream()
                    .content()
                    .doOnNext(chunk -> {
                        try {
                            thinkingContent.append(chunk);
                            emitter.send(SseEmitter.event().name("thinking").data(chunk));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            emitter.send(SseEmitter.event().name("thinking_done").data(""));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .blockLast();

            String answerSystemPrompt = conversation.getSystemPrompt() != null && !conversation.getSystemPrompt().isBlank()
                    ? conversation.getSystemPrompt()
                    : "你是一个友好、专业的AI助手。";

            if (StrUtil.isNotBlank(attachmentContext)) {
                answerSystemPrompt = answerSystemPrompt + "\n\n" + attachmentContext;
            }

            String answerUserMsg = String.format("""
                    用户问题：%s
                    
                    思考过程：%s
                    
                    请基于以上思考，给出最终回答。注意：回答要简洁自然，不要重复思考过程。
                    """, message, thinkingContent);

            var answerPromptBuilder = chatClient.prompt()
                    .system(answerSystemPrompt)
                    .user(answerUserMsg)
                    .advisors(advisor ->
                            advisor.param(ChatMemory.CONVERSATION_ID, String.valueOf(conversationId)
                            ));
            answerPromptBuilder.stream()
                    .content()
                    .doOnNext(chunk -> {
                        try {
                            emitter.send(SseEmitter.event().name("answer").data(chunk));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .doOnComplete(() -> {
                        updateMessageAttachments(conversationId, attachments);
                        try {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (IOException ignored) {
                        }
                    })
                    .blockLast();
        });
        return emitter;
    }

    public SseEmitter doChatStream(Long conversationId, String message, ToolCallback[] toolCallbacks) {
        return doChatStream(conversationId, message, toolCallbacks, null);
    }

    public SseEmitter doChatStream(Long conversationId, String message, ToolCallback[] toolCallbacks, List<ChatMessageSendRequest.AttachmentInfo> attachments) {

        if (ObjUtil.isNull(conversationId) || StrUtil.isBlank(message)) {
            throw new RuntimeException("会话ID或消息不能为空");
        }
        ChatConversation conversation = chatConversationService.getById(conversationId);
        if (ObjectUtil.isEmpty(conversation)) {
            throw new RuntimeException("会话不存在");
        }

        if ("新会话".equals(conversation.getTitle()) && StrUtil.isNotBlank(message)) {
            String newTitle = message.length() <= 20 ? message : message.substring(0, 20) + "...";
            conversation.setTitle(newTitle);
            chatConversationService.updateById(conversation);
        }

        String systemPromptBase = StrUtil.isBlank(conversation.getSystemPrompt()) ? "请给出回答" : conversation.getSystemPrompt();
        String attachmentContext = buildAttachmentContext(attachments);
        final String systemPrompt = StrUtil.isNotBlank(attachmentContext)
                ? systemPromptBase + "\n\n" + attachmentContext
                : systemPromptBase;

        var promptBuilder = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(advisor ->
                        advisor.param(ChatMemory.CONVERSATION_ID, String.valueOf(conversationId)
                        ));

        if (toolCallbacks != null && toolCallbacks.length > 0) {
            promptBuilder = promptBuilder.toolCallbacks(toolCallbacks);
        }

        Flux<String> stringFlux = promptBuilder.stream()
                .content()
                .doOnSubscribe(subscription -> log.info("会话 {} 开始", conversationId))
                .doOnError(e -> log.error("会话 {} 发生错误：{}", conversationId, e.getMessage()))
                .doOnComplete(() -> log.info("会话 {} 完成", conversationId));

        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                stringFlux.doOnNext(chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("message").data(chunk));
                            } catch (IOException e) {
                                throw new RuntimeException("Emitter断连" + e.getMessage());
                            }
                        })
                        .doOnComplete(() -> {
                            updateMessageAttachments(conversationId, attachments);
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (IOException ignored) {
                            }
                        })
                        .doOnError(e -> {
                            try {
                                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                                emitter.complete();
                            } catch (IOException ignored) {
                            }
                        })
                        .subscribe();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ignored) {
                }
            }
        });
        return emitter;

    }

    private String buildAttachmentContext(List<ChatMessageSendRequest.AttachmentInfo> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## 重要：用户上传了以下附件\n");
        sb.append("你必须基于这些附件内容来回答用户的问题。如果用户的问题与附件相关，你的回答必须引用和基于附件中的具体内容，而不是泛泛而谈。\n\n");
        for (ChatMessageSendRequest.AttachmentInfo att : attachments) {
            sb.append("### 附件：").append(att.getName()).append("\n");
            String content = att.getParsedContent();
            if (StrUtil.isBlank(content) && StrUtil.isNotBlank(att.getUrl())) {
                content = fileContentCache.get(att.getUrl());
            }
            if (StrUtil.isBlank(content) && StrUtil.isNotBlank(att.getUrl()) && StrUtil.isNotBlank(att.getType())) {
                log.info("缓存未命中，从OSS下载并解析: url={}, type={}", att.getUrl(), att.getType());
                content = fileParseService.parseFromOss(att.getUrl(), att.getType());
                if (StrUtil.isNotBlank(content)) {
                    fileContentCache.put(att.getUrl(), content);
                }
            }
            if (StrUtil.isNotBlank(content)) {
                sb.append("```\n").append(content).append("\n```\n");
            } else {
                sb.append("(无法解析该文件内容)\n");
            }
        }
        sb.append("\n请务必基于以上附件内容回答用户的问题。");
        return sb.toString();
    }

    private void updateMessageAttachments(Long conversationId, List<ChatMessageSendRequest.AttachmentInfo> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        try {
            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getConversationId, conversationId)
                    .eq(ChatMessage::getRole, "user")
                    .orderByDesc(ChatMessage::getId)
                    .last("LIMIT 1");
            ChatMessage userMsg = chatMessageService.getOne(wrapper, false);
            if (userMsg != null) {
                var attachmentData = attachments.stream()
                        .map(a -> {
                            var map = new java.util.LinkedHashMap<String, Object>();
                            map.put("name", a.getName());
                            map.put("type", a.getType());
                            map.put("url", a.getUrl());
                            map.put("size", a.getSize());
                            return map;
                        })
                        .collect(Collectors.toList());
                userMsg.setAttachments(JSONUtil.toJsonStr(attachmentData));
                chatMessageService.updateById(userMsg);
            }
        } catch (Exception e) {
            log.error("更新消息附件信息失败: {}", e.getMessage());
        }
    }
}
