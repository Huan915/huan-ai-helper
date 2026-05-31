package com.huan.aihelper.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String systemPrompt;

    @TableField("conversation_type")
    private String conversationType;

    private String modelName;

    private Integer contextWindowSize;

    private String contextStrategy;

    private BigDecimal temperature;

    private BigDecimal topP;

    private Integer status;

    private Long totalTokens;

    private Integer totalMessages;

    private LocalDateTime lastMessageTime;

    private String agentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
