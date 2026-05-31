package com.huan.aihelper.controller;

import com.huan.aihelper.common.Result;
import com.huan.aihelper.model.entity.AgentConfig;
import com.huan.aihelper.service.AgentConfigService;
import com.huan.aihelper.service.RagAgentAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "RAG智能体管理（管理员）")
@RestController
@RequestMapping("/admin/rag-agent")
public class RagAgentAdminController {

    @Resource
    private RagAgentAdminService ragAgentAdminService;

    @Resource
    private AgentConfigService agentConfigService;

    @Operation(summary = "创建RAG智能体")
    @PostMapping("/create")
    public Result<AgentConfig> createRagAgent(
            @RequestParam String agentName,
            @RequestParam String systemPrompt,
            @RequestParam(required = false) String description,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam("files") MultipartFile[] files) {
        try {
            AgentConfig agentConfig = ragAgentAdminService.createRagAgent(agentName, description, systemPrompt, avatar, files);
            return Result.success(agentConfig);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "查看所有RAG智能体")
    @GetMapping("/list")
    public Result<List<AgentConfig>> listRagAgents() {
        try {
            List<AgentConfig> list = agentConfigService.listActive();
            return Result.success(list);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取RAG智能体详情")
    @GetMapping("/{agentId}")
    public Result<AgentConfig> getRagAgent(@PathVariable String agentId) {
        try {
            AgentConfig agent = agentConfigService.getByAgentId(agentId);
            if (agent == null) {
                return Result.error("智能体不存在");
            }
            return Result.success(agent);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除RAG智能体")
    @DeleteMapping("/{agentId}")
    public Result<Boolean> deleteRagAgent(@PathVariable String agentId) {
        try {
            AgentConfig agent = agentConfigService.getByAgentId(agentId);
            if (agent == null) {
                return Result.error("智能体不存在");
            }
            agentConfigService.removeById(agent.getId());
            return Result.success(true);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "修改RAG智能体配置")
    @PutMapping("/{agentId}")
    public Result<AgentConfig> updateRagAgent(
            @PathVariable String agentId,
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String systemPrompt,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        try {
            AgentConfig agent = agentConfigService.getByAgentId(agentId);
            if (agent == null) {
                return Result.error("智能体不存在");
            }
            if (agentName != null) {
                agent.setAgentName(agentName);
            }
            if (description != null) {
                agent.setDescription(description);
            }
            if (systemPrompt != null) {
                agent.setSystemPrompt(systemPrompt);
            }
            if (avatar != null && !avatar.isEmpty()) {
                String avatarUrl = ragAgentAdminService.updateAvatar(agentId, avatar);
                agent.setAvatarUrl(avatarUrl);
            }
            agentConfigService.updateById(agent);
            return Result.success(agent);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}