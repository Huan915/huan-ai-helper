package com.huan.aihelper.app;

import com.huan.aihelper.advisor.MyLoggerAdvisor;
import com.huan.aihelper.agent.HuanAgent;
import com.huan.aihelper.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.yaml.snakeyaml.emitter.Emitter;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class TestApp {

    private final ChatClient chatClient;

    private static final String systemPrompt = """
            You are an AI agent. Please respond based on the user's input and using the available tools. You need to reply in the same language as the user's input.
            """;
    @Resource
    private HuanAgent huanAgent;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public TestApp(ChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(
                                new FileBasedChatMemory("chat-memory", 3)
                        ).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }
    // 简单测试的接口
    public String doChat(String message) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("用户输入：{}", message);
        log.info("Ai回复：{}，\nToken消耗量：{}", content, chatResponse.getMetadata().getUsage().getTotalTokens());
        return content;
    }

    public Flux<String> doChatByStream(String message) {
        return  chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(message)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        assert chatResponse != null;
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }

    public record ActorsFilms(String actor, List<String> movies) {
    }

    public ActorsFilms doChatWithStruct(String message, String chatId) {

        ActorsFilms actorsFilms = chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(message)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .entity(ActorsFilms.class);

        return actorsFilms;
    }

    @Resource
    private VectorStore pgVectorStore;

    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(QuestionAnswerAdvisor.builder(pgVectorStore).build())
                .user(message)
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        assert chatResponse != null;
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }

    public SseEmitter doChatWithAgent(String message) {
        return huanAgent.runSseEmitter(message);
    }

}