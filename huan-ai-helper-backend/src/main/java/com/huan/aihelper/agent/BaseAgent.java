package com.huan.aihelper.agent;

import cn.hutool.core.util.StrUtil;
import com.huan.aihelper.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *  抽象基类，用于管理状态和执行流程
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt = "";
    private String nextStepPrompt = "";

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxStep = 10;

    private ChatClient chatClient;

    // 自主维护会话上下文
    private List<Message> messageList = new ArrayList<>();

    public String run(String userPrompt) {
        if (state != AgentState.IDLE) {
            throw new RuntimeException("智能体无法执行，智能体状态：" + state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("UserPrompt cannot null");
        }
        // 执行，更改状态
        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));

        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i  <= maxStep && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {} / {}", stepNumber, maxStep);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxStep) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxStep + ")");
            }
            return String.join("\n", results);
        }catch (Exception e){
            this.state = AgentState.ERROR;
            log.error(e.getMessage(), e);
            return "执行错误" + e.getMessage();
        }finally {
            // 3、清理资源
            this.cleanup();
        }

    }

    public SseEmitter runSseEmitter(String userPrompt) {
        SseEmitter emitter = new SseEmitter(300000L); //5 min timeout

        // 开启异步处理
        CompletableFuture.runAsync(() -> {
            try {
                if (state != AgentState.IDLE) {
                    emitter.send("智能体无法执行，智能体状态：" + state);
                    emitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.send("智能体提示词不能为空");
                    emitter.complete();
                    return;
                }
            }catch (Exception ex){
                emitter.completeWithError(ex);
            }
            // 执行，更改状态
            this.state = AgentState.RUNNING;
            messageList.add(new UserMessage(userPrompt));

            List<String> results = new ArrayList<>();
            try {
                for (int i = 0; i  <= maxStep && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {} / {}", stepNumber, maxStep);
                    // 单步执行
                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);
                    emitter.send(result);
                }
                // 检查是否超出步骤限制
                if (currentStep >= maxStep) {
                    state = AgentState.FINISHED;
                    results.add("Terminated: Reached max steps (" + maxStep + ")");
                    emitter.send("执行结束：达到最大步骤：" + maxStep);
                }
                emitter.complete();
            }catch (Exception e){
                this.state = AgentState.ERROR;
                log.error(e.getMessage(), e);
                try {
                    emitter.send("执行错误" + e.getMessage());
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }finally {
                // 3、清理资源
                this.cleanup();
            }
        });
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE request timed out");
        });
        emitter.onCompletion( () -> {
            if (this.state.equals(AgentState.RUNNING)) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.warn("SSE request completed");
        });
        return emitter;
    }

    public abstract void cleanup();

    public abstract String step ();
}
