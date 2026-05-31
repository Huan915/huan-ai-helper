package com.huan.aihelper.controller;

import com.huan.aihelper.app.TestApp;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TestApp testApp;

    /**
     *  同步调用接口
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/chat/sync")
    public String chat(String message, String chatId){
        return testApp.doChat(message);
    }

    /**
     *  SSE 流式传输
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatByStream(String message, String chatId){
        return testApp.doChatByStream(message);
    }

    /**
     *  Agent调用 （SSE流式传输）
     */
    @GetMapping(value = "/chat/agent/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatAgentByStream(String message){
        return testApp.doChatWithAgent(message);
    }


}
