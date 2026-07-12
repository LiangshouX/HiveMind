package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.application.SandboxDashboardService;
import com.liangshou.agentic.application.dto.SandboxHealthDTO;
import com.liangshou.agentic.application.dto.SandboxInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 沙箱管理面板 REST 控制器。
 *
 * <p>提供沙箱运行时的查询和管理接口，供前端沙箱面板使用。</p>
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
    public ResponseEntity<List<SandboxInfoDTO>> listSandboxes() {
        return ResponseEntity.ok(dashboardService.listActiveSandboxes());
    }

    /**
     * 获取沙箱健康摘要。
     */
    @GetMapping("/health")
    public ResponseEntity<SandboxHealthDTO> getHealth() {
        return ResponseEntity.ok(dashboardService.getHealth());
    }

    /**
     * 获取单个沙箱详情。
     */
    @GetMapping("/{containerId}")
    public ResponseEntity<SandboxInfoDTO> getSandbox(@PathVariable String containerId) {
        SandboxInfoDTO info = dashboardService.getSandboxDetail(containerId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    /**
     * 停止指定沙箱。
     */
    @PostMapping("/{containerId}/stop")
    public ResponseEntity<Map<String, Object>> stopSandbox(@PathVariable String containerId) {
        boolean success = dashboardService.stopSandbox(containerId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    /**
     * 删除指定沙箱。
     */
    @PostMapping("/{containerId}/remove")
    public ResponseEntity<Map<String, Object>> removeSandbox(@PathVariable String containerId) {
        boolean success = dashboardService.removeSandbox(containerId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    /**
     * 清理所有沙箱。
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupAll() {
        int removed = dashboardService.cleanupAll();
        return ResponseEntity.ok(Map.of("removed", removed));
    }
}
