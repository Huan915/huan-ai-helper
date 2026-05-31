package com.huan.aihelper.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.huan.aihelper.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具列表
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
    }
    @Override
    public boolean think() {
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);
            try {
                ChatResponse chatResponse = getChatClient().prompt(prompt)
                        .system(getSystemPrompt())
                        .toolCallbacks(availableTools)
                        .call()
                        .chatResponse();
                toolCallChatResponse = chatResponse;
                AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
                List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
                String result = assistantMessage.getText();
                log.info("{}的思考：{}，\n选择了{}个工具来使用", getName(), result, toolCallList.size());
                String toolCallInfo = toolCallList.stream()
                        .map(toolCall -> String.format("工具名称：%s， 参数：%s", toolCall.name(), toolCall.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfo);
                if (toolCallList.isEmpty()) {
                    // 只有不调用工具时，才需要手动记录助手消息
                    getMessageList().add(assistantMessage);
                    return false;
                } else {
                    // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                    return true;
                }
            }catch (Exception e) {
                log.error("工具调用失败：{}", e.getMessage());
                getMessageList().add(new AssistantMessage(e.getMessage()));
                return false;
            }

    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "不需要调用工具";
        }
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        // 调用工具
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，工具调用结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具;
        boolean isTerminate = toolResponseMessage.getResponses().stream().
                anyMatch(response -> "doTerminate".equals(response.name()));
        if (isTerminate) {
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> String.format("工具名称：%s, 返回：%s", response.name(), response.responseData()))
                .collect(Collectors.joining("\n"));
        log.info("工具调用结果：{}", results);
        return results;
    }

    @Override
    public void cleanup() {

    }
}
