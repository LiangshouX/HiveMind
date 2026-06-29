package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.application.IToolConfigService;
import com.liangshou.agentic.application.dto.ToolConfigDTO;
import com.liangshou.agentic.common.util.SecurityUtils;
import com.liangshou.agentic.domain.tool.model.ToolConfigUpdateCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工具配置 REST 控制器。
 *
 * <p>提供工具配置的查询、更新和同步接口，支持按用户隔离管理工具。</p>
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/agent/tool-config")
public class ToolConfigController {

    private final IToolConfigService toolConfigService;

    public ToolConfigController(IToolConfigService toolConfigService) {
        this.toolConfigService = toolConfigService;
    }

    /**
     * 获取当前用户的所有工具配置。
     *
     * @return 工具配置列表
     */
    @GetMapping
    public ResponseEntity<List<ToolConfigDTO>> listToolConfigs() {
        String userId = SecurityUtils.getCurrentUserId();
        List<ToolConfigDTO> configs = toolConfigService.listByUserId(userId);
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取单个工具配置详情。
     *
     * @param toolName 工具名称
     * @return 工具配置
     */
    @GetMapping("/{toolName}")
    public ResponseEntity<ToolConfigDTO> getToolConfig(@PathVariable String toolName) {
        String userId = SecurityUtils.getCurrentUserId();
        ToolConfigDTO config = toolConfigService.getToolConfig(userId, toolName);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * 更新工具配置。
     *
     * @param toolName 工具名称
     * @param command  更新命令
     * @return 更新后的配置
     */
    @PutMapping("/{toolName}")
    public ResponseEntity<ToolConfigDTO> updateToolConfig(
            @PathVariable String toolName,
            @RequestBody ToolConfigUpdateCommand command) {
        String userId = SecurityUtils.getCurrentUserId();
        ToolConfigDTO updated = toolConfigService.updateToolConfig(userId, toolName, command);
        return ResponseEntity.ok(updated);
    }

    /**
     * 同步系统新增工具到用户配置。
     *
     * @return 同步的工具数量
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncSystemTools() {
        String userId = SecurityUtils.getCurrentUserId();
        int synced = toolConfigService.syncSystemTools(userId);
        return ResponseEntity.ok(Map.of(
                "synced", synced,
                "message", synced > 0 ? "已同步 " + synced + " 个新工具" : "无新增工具"
        ));
    }

    /**
     * 查看系统默认工具清单（不创建用户配置）。
     *
     * @return 系统默认工具列表
     */
    @GetMapping("/system-defaults")
    public ResponseEntity<List<ToolConfigDTO>> listSystemDefaults() {
        String userId = SecurityUtils.getCurrentUserId();
        // 获取系统工具但不保存到用户配置
        List<ToolConfigDTO> configs = toolConfigService.listByUserId(userId);
        return ResponseEntity.ok(configs);
    }
}
