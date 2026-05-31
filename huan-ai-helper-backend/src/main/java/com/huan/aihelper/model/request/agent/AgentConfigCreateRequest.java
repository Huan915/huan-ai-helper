package com.huan.aihelper.model.request.agent;

import lombok.Data;

@Data
public class AgentConfigCreateRequest {

    private String agentName;

    private String description;

    private String systemPrompt;
}