package com.huan.aihelper.tools;

import cn.hutool.http.HttpUtil;
import org.springframework.ai.tool.annotation.Tool;

public class CurrentTimeTool {
    @Tool(description = "Get the current real-time time")
    public String getCurrentTime() {
        String result = HttpUtil.get("https://uapis.cn/api/v1/misc/worldtime?city=Asia/Shanghai");
        return result;
    }
}
