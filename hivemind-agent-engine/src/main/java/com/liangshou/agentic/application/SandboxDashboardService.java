package com.liangshou.agentic.application;

import com.liangshou.agentic.agents.sandbox.TdAgentSandboxManager;
import com.liangshou.agentic.application.dto.SandboxHealthDTO;
import com.liangshou.agentic.application.dto.SandboxInfoDTO;
import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 沙箱管理面板服务 - 提供沙箱运行时的查询和管理能力。
 *
 * <p>包装 {@link TdAgentSandboxManager} 和 AgentScope SDK 的 {@link SandboxService}，
 * 将底层容器管理 API 转换为面向前端的 DTO。</p>
 *
 * @author LiangshouX
 */
@Service
public class SandboxDashboardService {

    private static final Logger log = LoggerFactory.getLogger(SandboxDashboardService.class);

    private final TdAgentSandboxManager sandboxManager;
    private final TdAgentProperties properties;

    public SandboxDashboardService(TdAgentSandboxManager sandboxManager, TdAgentProperties properties) {
        this.sandboxManager = sandboxManager;
        this.properties = properties;
    }

    /**
     * 列出所有活跃沙箱。
     */
    public List<SandboxInfoDTO> listActiveSandboxes() {
        SandboxService service = sandboxManager.getSandboxService();
        if (service == null) {
            return Collections.emptyList();
        }

        Map<String, ContainerModel> all = service.getAllSandboxes();
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }

        return all.entrySet().stream()
                .map(entry -> toInfoDTO(entry.getKey(), entry.getValue(), service))
                .collect(Collectors.toList());
    }

    /**
     * 获取单个沙箱详情。
     */
    public SandboxInfoDTO getSandboxDetail(String containerId) {
        SandboxService service = sandboxManager.getSandboxService();
        if (service == null) return null;

        ContainerModel model = service.getSandbox(containerId);
        if (model == null) return null;

        return toInfoDTO(containerId, model, service);
    }

    /**
     * 获取沙箱健康摘要。
     */
    public SandboxHealthDTO getHealth() {
        boolean enabled = properties.getSandbox().isEnabled();

        if (!enabled) {
            return SandboxHealthDTO.builder()
                    .sandboxEnabled(false)
                    .dockerConnected(false)
                    .errorMessage("沙箱功能未启用")
                    .totalSandboxes(0)
                    .runningCount(0)
                    .stoppedCount(0)
                    .build();
        }

        // 探测 Docker 连接
        boolean dockerOk = sandboxManager.isDockerReachable();
        if (!dockerOk) {
            return SandboxHealthDTO.builder()
                    .sandboxEnabled(true)
                    .dockerConnected(false)
                    .errorMessage("Docker 服务未运行或无法连接，请先启动 Docker Desktop 或 Docker daemon")
                    .totalSandboxes(0)
                    .runningCount(0)
                    .stoppedCount(0)
                    .build();
        }

        SandboxService service = sandboxManager.getSandboxService();
        if (service == null) {
            return SandboxHealthDTO.builder()
                    .sandboxEnabled(true)
                    .dockerConnected(true)
                    .errorMessage("SandboxService 初始化失败")
                    .totalSandboxes(0)
                    .runningCount(0)
                    .stoppedCount(0)
                    .build();
        }

        Map<String, ContainerModel> all = service.getAllSandboxes();
        if (all == null || all.isEmpty()) {
            return SandboxHealthDTO.builder()
                    .sandboxEnabled(true)
                    .dockerConnected(true)
                    .totalSandboxes(0)
                    .runningCount(0)
                    .stoppedCount(0)
                    .build();
        }

        int running = 0;
        int stopped = 0;
        for (String id : all.keySet()) {
            String status = service.getSandboxStatus(id);
            if (isRunning(status)) {
                running++;
            } else {
                stopped++;
            }
        }

        return SandboxHealthDTO.builder()
                .sandboxEnabled(true)
                .dockerConnected(true)
                .totalSandboxes(all.size())
                .runningCount(running)
                .stoppedCount(stopped)
                .build();
    }

    /**
     * 停止指定沙箱。
     */
    public boolean stopSandbox(String containerId) {
        SandboxService service = sandboxManager.getSandboxService();
        if (service == null) return false;

        try {
            service.stopSandbox(containerId);
            log.info("[SandboxDashboard] 已停止沙箱: {}", containerId);
            return true;
        } catch (Exception e) {
            log.error("[SandboxDashboard] 停止沙箱失败: {}", containerId, e);
            return false;
        }
    }

    /**
     * 删除指定沙箱。
     */
    public boolean removeSandbox(String containerId) {
        SandboxService service = sandboxManager.getSandboxService();
        if (service == null) return false;

        try {
            boolean result = service.removeSandbox(containerId);
            log.info("[SandboxDashboard] 已删除沙箱: {}, result: {}", containerId, result);
            return result;
        } catch (Exception e) {
            log.error("[SandboxDashboard] 删除沙箱失败: {}", containerId, e);
            return false;
        }
    }

    /**
     * 清理所有沙箱。
     */
    public int cleanupAll() {
        SandboxService service = sandboxManager.getSandboxService();
        if (service == null) return 0;

        Map<String, ContainerModel> all = service.getAllSandboxes();
        if (all == null || all.isEmpty()) return 0;

        int count = all.size();
        try {
            service.cleanupAllSandboxes();
            log.info("[SandboxDashboard] 已清理所有沙箱, 数量: {}", count);
        } catch (Exception e) {
            log.error("[SandboxDashboard] 清理沙箱失败", e);
        }
        return count;
    }

    /**
     * 将 ContainerModel 转换为 SandboxInfoDTO。
     */
    private SandboxInfoDTO toInfoDTO(String containerId, ContainerModel model, SandboxService service) {
        String sandboxType = inferSandboxType(model.getVersion());
        String status;
        try {
            status = service.getSandboxStatus(containerId);
        } catch (Exception e) {
            status = "unknown";
        }

        return SandboxInfoDTO.builder()
                .containerId(containerId)
                .containerName(model.getContainerName())
                .sandboxType(sandboxType)
                .status(status)
                .ports(model.getPorts())
                .baseUrl(model.getBaseUrl())
                .browserUrl(model.getBrowserUrl())
                .mountDir(model.getMountDir())
                .version(model.getVersion())
                .refCount(getRefCount(service, containerId))
                .providedTools(getToolsForType(sandboxType))
                .build();
    }

    /**
     * 从 Docker 镜像名推断沙箱类型。
     */
    private String inferSandboxType(String version) {
        if (version == null) return "unknown";
        String lower = version.toLowerCase();
        if (lower.contains("browser")) return "browser";
        if (lower.contains("filesystem")) return "filesystem";
        if (lower.contains("gui")) return "gui";
        if (lower.contains("mobile")) return "mobile";
        if (lower.contains("appworld") || lower.contains("bfcl")) return "training";
        return "base";
    }

    /**
     * 安全获取引用计数。
     */
    private long getRefCount(SandboxService service, String containerId) {
        try {
            // SandboxMap 内部维护 refCount，通过 getAllSandboxes 间接访问
            // ContainerModel 本身没有 refCount 字段，这里返回 1 作为默认值
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 根据沙箱类型返回该类型提供的工具名称列表。
     */
    private List<String> getToolsForType(String sandboxType) {
        return switch (sandboxType) {
            case "browser" -> List.of(
                    "browser_navigate", "browser_snapshot", "browser_click",
                    "browser_type", "browser_wait_for", "browser_take_screenshot"
            );
            case "filesystem" -> List.of(
                    "fs_read_file", "fs_write_file", "edit_file",
                    "move_file", "list_directory", "search_files"
            );
            case "base" -> List.of("run_ipython_cell", "run_shell_command");
            default -> List.of();
        };
    }

    /**
     * 判断沙箱状态是否为"运行中"。
     */
    private boolean isRunning(String status) {
        if (status == null) return false;
        String lower = status.toLowerCase();
        return lower.equals("running")
                || lower.equals("created")
                || lower.equals("partiallyready")
                || lower.equals("pending")
                || lower.equals("starting");
    }
}
