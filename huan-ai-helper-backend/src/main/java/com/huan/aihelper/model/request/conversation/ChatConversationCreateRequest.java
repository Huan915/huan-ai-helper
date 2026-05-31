package com.huan.aihelper.model.request.conversation;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChatConversationCreateRequest {

    private Long userId;

    private String title;

    private String systemPrompt;

    private String conversationType;

    private String modelName;

    private Integer contextWindowSize;

    private String contextStrategy;

    private BigDecimal temperature;

    private BigDecimal topP;

    private String agentId;
}
