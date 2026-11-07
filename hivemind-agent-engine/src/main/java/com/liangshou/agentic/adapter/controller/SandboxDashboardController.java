package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.application.SandboxDashboardService;
import com.liangshou.agentic.application.dto.SandboxHealthDTO;
import com.liangshou.agentic.application.dto.SandboxInfoDTO;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import com.liangshou.agentic.common.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 沙箱管理面板 REST 控制器。
 *
 * <p>提供沙箱运行时的查询和管理接口，供前端沙箱面板使用。
 * 所有端点统一返回 {@link Result} 包装。</p>
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/agent/sandboxes")
public class SandboxDashboardController {

    private final SandboxDashboardService dashboardService;

    public SandboxDashboardController(SandboxDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取所有活跃沙箱列表。
     */
    @GetMapping
    public Result<List<SandboxInfoDTO>> listSandboxes() {
        return Result.success(dashboardService.listActiveSandboxes());
    }

    /**
     * 获取沙箱健康摘要。
     */
    @GetMapping("/health")
    public Result<SandboxHealthDTO> getHealth() {
        return Result.success(dashboardService.getHealth());
    }

    /**
     * 获取单个沙箱详情。
     */
    @GetMapping("/{containerId}")
    public Result<SandboxInfoDTO> getSandbox(@PathVariable String containerId) {
        SandboxInfoDTO info = dashboardService.getSandboxDetail(containerId);
        if (info == null) {
            throw new BizException(HmeErrorCode.PARAM_ERROR, "沙箱不存在: " + containerId);
        }
        return Result.success(info);
    }

    /**
     * 停止指定沙箱。
     */
    @PostMapping("/{containerId}/stop")
    public Result<Map<String, Object>> stopSandbox(@PathVariable String containerId) {
        return Result.success(Map.of("success", dashboardService.stopSandbox(containerId)));
    }

    /**
     * 删除指定沙箱。
     */
    @PostMapping("/{containerId}/remove")
    public Result<Map<String, Object>> removeSandbox(@PathVariable String containerId) {
        return Result.success(Map.of("success", dashboardService.removeSandbox(containerId)));
    }

    /**
     * 清理所有沙箱。
     */
    @PostMapping("/cleanup")
    public Result<Map<String, Object>> cleanupAll() {
        return Result.success(Map.of("removed", dashboardService.cleanupAll()));
    }
}
