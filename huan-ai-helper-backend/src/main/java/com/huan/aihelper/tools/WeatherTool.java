package com.huan.aihelper.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class WeatherTool {
    @Tool(description = "This is a tool for querying the real-time weather conditions of a specific area.")
    String getCurrentWeather(@ToolParam(description = "The area being queried") String region) {
        return "36";
    }
}

