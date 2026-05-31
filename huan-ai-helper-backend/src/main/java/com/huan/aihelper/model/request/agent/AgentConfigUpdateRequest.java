package com.huan.aihelper.model.request.agent;

import lombok.Data;

@Data
public class AgentConfigUpdateRequest {

    private String agentName;

    private String description;

    private String systemPrompt;

    private Integer status;
}