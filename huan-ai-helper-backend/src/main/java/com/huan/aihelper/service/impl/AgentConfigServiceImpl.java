package com.huan.aihelper.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huan.aihelper.mapper.AgentConfigMapper;
import com.huan.aihelper.model.entity.AgentConfig;
import com.huan.aihelper.service.AgentConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentConfigServiceImpl extends ServiceImpl<AgentConfigMapper, AgentConfig> implements AgentConfigService {

    @Override
    public AgentConfig getByAgentId(String agentId) {
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConfig::getAgentId, agentId);
        return getOne(wrapper);
    }

    @Override
    public List<AgentConfig> listActive() {
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConfig::getStatus, 0)
                .orderByDesc(AgentConfig::getCreatedAt);
        return list(wrapper);
    }
}