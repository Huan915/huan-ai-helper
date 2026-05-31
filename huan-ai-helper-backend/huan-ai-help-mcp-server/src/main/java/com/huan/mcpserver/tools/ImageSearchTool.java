package com.huan.mcpserver.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ImageSearchTool {

    private static final String apiKey = "";
    private static final String BASE_URL = "https://api.pexels.com/v1/search";

    @Tool(description = "search image from web")
    public List<String> searchMediumImages(@ToolParam(description = "Search query keyword") String query) {
        List<String> imageUrls = new ArrayList<>();

        try {
            HttpResponse response = HttpRequest.get(BASE_URL)
                    .header("Authorization", apiKey)
                    .form("query", query)
                    .form("size", "medium")
                    .form("per_page", 10)
                    .timeout(10000)
                    .execute();

            if (!response.isOk()) {
                log.error("Pexels API 请求失败: {}", response.getStatus());
                return imageUrls;
            }

            JSONObject jsonObject = JSONUtil.parseObj(response.body());
            JSONArray photos = jsonObject.getJSONArray("photos");

            if (photos != null) {
                for (int i = 0; i < photos.size(); i++) {
                    JSONObject photo = photos.getJSONObject(i);
                    JSONObject src = photo.getJSONObject("src");
                    if (src != null) {
                        String mediumUrl = src.getStr("medium");
                        if (mediumUrl != null && !mediumUrl.isEmpty()) {
                            imageUrls.add(mediumUrl);
                        }
                    }
                }
            }

            log.info("搜索成功: query={}, 找到{}张图片", query, imageUrls.size());

        } catch (Exception e) {
            log.error("图片搜索失败: {}", e.getMessage(), e);
        }

        return imageUrls;
    }
}
