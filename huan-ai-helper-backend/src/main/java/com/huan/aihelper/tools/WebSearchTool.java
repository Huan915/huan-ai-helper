package com.huan.aihelper.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@Slf4j
public class WebSearchTool {

    private static final String BASE_URL = "https://qianfan.baidubce.com/v2/ai_search/chat/completions";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search the web for real-time information based on user query and return an intelligent summary")
    public String webSearch(@ToolParam(description = "The search query text to look up on the web") String query) {
        try {
            JSONObject body = new JSONObject();

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.set("role", "user");
            message.set("content", query);
            messages.add(message);

            body.set("messages", messages);
            body.set("model", "ernie-4.5-turbo-32k");
            body.set("stream", false);
            body.set("search_source", "baidu_search_v2");
            body.set("enable_corner_markers", false);

            HttpResponse response = HttpRequest.post(BASE_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000)
                    .execute();

            if (!response.isOk()) {
                log.error("百度智能搜索请求失败: status={}, body={}", response.getStatus(), response.body());
                return "搜索请求失败，状态码: " + response.getStatus();
            }

            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray choices = result.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject messageObj = choice.getJSONObject("message");
                if (messageObj != null) {
                    String content = messageObj.getStr("content");
                    log.info("搜索成功: query={}", query);
                    return content;
                }
            }

            return "未找到相关搜索结果";
        } catch (Exception e) {
            log.error("网页搜索失败: {}", e.getMessage(), e);
            return "搜索失败: " + e.getMessage();
        }
    }
}
