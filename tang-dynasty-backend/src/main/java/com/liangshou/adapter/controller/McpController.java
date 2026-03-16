package com.liangshou.adapter.controller;

import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.McpPO;
import com.liangshou.service.McpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MCP 管理控制器
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {
    
    private final McpService mcpService;

    @GetMapping
    public Result<List<McpPO>> listMcps() {
        return Result.success(mcpService.list());
    }

    @PostMapping
    public Result<Boolean> saveMcp(@RequestBody McpPO mcp) {
        if (mcp.getId() == null) {
            mcp.setCreateTime(LocalDateTime.now());
        }
        mcp.setUpdateTime(LocalDateTime.now());
        return Result.success(mcpService.saveOrUpdate(mcp));
    }
    
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteMcp(@PathVariable Long id) {
        return Result.success(mcpService.removeById(id));
    }
    
    // --- Call MCP Tool ---
    @PostMapping("/{mcpName}/tools/{toolName}/call")
    public Result<Object> callTool(
            @PathVariable String mcpName,
            @PathVariable String toolName,
            @RequestBody(required = false) Map<String, Object> arguments
    ) {
        try {
            log.info("Calling MCP tool: {}.{}", mcpName, toolName);
            Object result = mcpService.callTool(mcpName, toolName, arguments != null ? arguments : Map.of());
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to call MCP tool: {}.{}", mcpName, toolName, e);
            return Result.error("Failed to call MCP tool: " + e.getMessage());
        }
    }
    
    // --- Get Active Clients ---
    @GetMapping("/active")
    public Result<List<String>> getActiveClients() {
        return Result.success(mcpService.getActiveClients());
    }
    
    // --- Enable/Disable MCP ---
    @PostMapping("/{id}/enable")
    public Result<Boolean> enableMcp(@PathVariable Long id) {
        try {
            mcpService.enableMcp(id);
            return Result.success(true);
        } catch (Exception e) {
            log.error("Failed to enable MCP: {}", id, e);
            return Result.error("Failed to enable MCP: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/disable")
    public Result<Boolean> disableMcp(@PathVariable Long id) {
        try {
            mcpService.disableMcp(id);
            return Result.success(true);
        } catch (Exception e) {
            log.error("Failed to disable MCP: {}", id, e);
            return Result.error("Failed to disable MCP: " + e.getMessage());
        }
    }
}
