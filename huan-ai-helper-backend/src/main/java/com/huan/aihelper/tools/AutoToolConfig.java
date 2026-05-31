package com.huan.aihelper.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoToolConfig {

    @Value("${baidu.ai-search.api-key}")
    private String apiKey;

    @Bean
    public ToolCallback[] allTool() {
        TerminateTool terminateTool = new TerminateTool();
        WebSearchTool webSearchTool = new WebSearchTool(apiKey);

        return ToolCallbacks.from(
                terminateTool,
                webSearchTool
        );
    }
}
