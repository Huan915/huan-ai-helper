package com.huan.aihelper.agent;

import com.huan.aihelper.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 *  Huan 的超级智能体, 拥有自主规划能力和工具调用能力，可直接使用。
 */
@Component
public class HuanAgent extends ToolCallAgent {

    public HuanAgent(ToolCallback[] allTool, ChatModel dashscopeChatModel) {
        super(allTool);
        this.setName("HuanAgent");
        String SYSTEM_PROMPT = """
                You are SuperAgent, name is HuanAgent, an all-capable AI assistant, aimed at solving any task presented by the user.
                The reply should be in the same language as the user's question.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxStep(20);
        // 初始化AI客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
