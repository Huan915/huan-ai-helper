package com.huan.aihelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huan.aihelper.model.entity.ChatContextSummary;

public interface ChatContextSummaryService extends IService<ChatContextSummary> {

    ChatContextSummary getLatestSummary(Long conversationId);
}
