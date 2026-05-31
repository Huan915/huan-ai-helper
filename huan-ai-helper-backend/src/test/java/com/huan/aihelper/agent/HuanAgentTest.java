package com.huan.aihelper.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HuanAgentTest {
    @Resource
    private HuanAgent huanAgent;

    @Test
    void test() {
        String userPrompt = """
                不知道今天东莞多少度，今天能不能过去玩
                """;
        String answer = huanAgent.run(userPrompt);
        System.out.println(answer);
    }
}