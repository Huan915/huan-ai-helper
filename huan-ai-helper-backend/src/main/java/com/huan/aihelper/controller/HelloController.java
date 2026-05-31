package com.huan.aihelper.controller;

import com.huan.aihelper.app.TestApp;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("hello")
public class HelloController {

    @Resource
    private TestApp testApp;

    @GetMapping("/chat")
    public String sayHello(@Parameter String message) {
        String content = testApp.doChat(message, "888");

        return content;
    }

}
