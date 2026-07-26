package com.liangshou.agentic.application;

import com.liangshou.agentic.agents.sandbox.TdAgentSandboxManager;
import com.liangshou.agentic.application.dto.SandboxHealthDTO;
import com.liangshou.agentic.application.dto.SandboxInfoDTO;
import com.liangshou.agentic.common.config.TdAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 沙箱管理面板服务 - 提供沙箱运行时的查询和管理能力。
 *
 * <p>基于 AgentScope 2.0 harness 模块，通过 {@link TdAgentSandboxManager} 管理 Docker 沙箱。</p>
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
        // AgentScope 2.0 harness 模块的沙箱通过 TdAgentSandboxManager 管理
        // 当前返回基础信息，后续可扩展为查询活跃容器列表
        return Collections.emptyList();
    }

    /**
     * 获取单个沙箱详情。
     */
    public SandboxInfoDTO getSandboxDetail(String containerId) {
        return null;
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

        boolean dockerOk = sandboxManager.isDockerReachable();
        if (!dockerOk) {
            return SandboxHealthDTO.builder()
                    .sandboxEnabled(true)
                    .dockerConnected(false)
                    .errorMessage("Docker 服务未运行或无法连接")
                    .totalSandboxes(0)
                    .runningCount(0)
                    .stoppedCount(0)
                    .build();
        }

        return SandboxHealthDTO.builder()
                .sandboxEnabled(true)
                .dockerConnected(true)
                .totalSandboxes(0)
                .runningCount(0)
                .stoppedCount(0)
                .build();
    }

    /**
     * 停止指定沙箱。
     */
    public boolean stopSandbox(String containerId) {
        log.info("[SandboxDashboard] 停止沙箱: {}", containerId);
        return false;
    }

    /**
     * 删除指定沙箱。
     */
    public boolean removeSandbox(String containerId) {
        log.info("[SandboxDashboard] 删除沙箱: {}", containerId);
        return false;
    }

    /**
     * 清理所有沙箱。
     */
    public int cleanupAll() {
        log.info("[SandboxDashboard] 清理所有沙箱");
        sandboxManager.destroy();
        return 0;
    }
}
