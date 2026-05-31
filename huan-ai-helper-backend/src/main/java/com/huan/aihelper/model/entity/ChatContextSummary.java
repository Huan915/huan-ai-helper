package com.huan.aihelper.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huan.aihelper.model.handler.LongArrayTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "chat_context_summary", autoResultMap = true)
public class ChatContextSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String summaryContent;

    @TableField(typeHandler = LongArrayTypeHandler.class)
    private List<Long> summarizedMsgIds;

    private Integer tokenCount;

    private Integer seqStart;

    private Integer seqEnd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer isDeleted;
}
