package com.huan.aihelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huan.aihelper.model.entity.AgentConfig;

import java.util.List;

public interface AgentConfigService extends IService<AgentConfig> {

    AgentConfig getByAgentId(String agentId);

    List<AgentConfig> listActive();
}