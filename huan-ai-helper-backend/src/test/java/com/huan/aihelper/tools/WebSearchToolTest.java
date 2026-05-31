package com.huan.aihelper.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebSearchToolTest {

    @Resource
    private WebSearchTool webSearchTool;

    @Test
    void webSearch() {
        String query = "现在最新的是iphone第几代";
        String result = webSearchTool.webSearch(query);
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

}