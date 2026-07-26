package com.liangshou.agentic.agents.sandbox;

import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sandbox 管理器 - 基于 AgentScope 2.0 harness 模块管理 Docker 沙箱生命周期。
 *
 * <p>使用 {@link DockerSandboxClient} 创建和管理 Docker 容器沙箱，
 * 为 Agent 提供隔离的代码执行环境。</p>
 *
 * @author LiangshouX
 */
@Component
public class TdAgentSandboxManager {

    private static final Logger log = LoggerFactory.getLogger(TdAgentSandboxManager.class);

    private final TdAgentProperties properties;
    private volatile DockerSandboxClient sandboxClient;
    private volatile Boolean dockerReachable;

    /** 活跃沙箱映射：sessionKey → Sandbox */
    private final Map<String, Sandbox> activeSandboxes = new ConcurrentHashMap<>();

    public TdAgentSandboxManager(TdAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取或创建沙箱客户端（懒加载）。
     */
    public DockerSandboxClient getSandboxClient() {
        if (!properties.getSandbox().isEnabled()) {
            return null;
        }
        if (sandboxClient == null) {
            synchronized (this) {
                if (sandboxClient == null) {
                    sandboxClient = new DockerSandboxClient();
                    log.info("[SandboxManager] DockerSandboxClient 已创建");
                }
            }
        }
        return sandboxClient;
    }

    /**
     * 为指定会话创建或获取沙箱实例。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 沙箱实例，如果沙箱未启用则返回 null
     */
    public Sandbox getOrCreateSandbox(String userId, String sessionId) {
        if (!properties.getSandbox().isEnabled()) {
            return null;
        }
        String key = userId + ":" + sessionId;
        return activeSandboxes.computeIfAbsent(key, k -> {
            try {
                DockerSandboxClient client = getSandboxClient();
                if (client == null) {
                    return null;
                }
                DockerSandboxClientOptions options = buildOptions();
                WorkspaceSpec spec = new WorkspaceSpec();
                Sandbox sandbox = client.create(spec, null, options);
                sandbox.start();
                log.info("[SandboxManager] 沙箱已创建并启动 - key: {}, sessionId: {}",
                        key, sandbox.getState().getSessionId());
                return sandbox;
            } catch (Exception e) {
                log.error("[SandboxManager] 创建沙箱失败 - key: {}", key, e);
                return null;
            }
        });
    }

    /**
     * 停止并移除指定会话的沙箱。
     */
    public void removeSandbox(String userId, String sessionId) {
        String key = userId + ":" + sessionId;
        Sandbox sandbox = activeSandboxes.remove(key);
        if (sandbox != null) {
            try {
                sandbox.close();
                log.info("[SandboxManager] 沙箱已关闭 - key: {}", key);
            } catch (Exception e) {
                log.warn("[SandboxManager] 关闭沙箱失败 - key: {}", key, e);
            }
        }
    }

    /**
     * 构建 Docker 沙箱配置选项。
     */
    private DockerSandboxClientOptions buildOptions() {
        DockerSandboxClientOptions options = new DockerSandboxClientOptions();
        // 使用默认镜像 ubuntu:22.04，可通过 TdAgentProperties.Sandbox 扩展配置
        return options;
    }

    /**
     * 检测 Docker 容器运行时是否可连接。
     */
    public boolean isDockerReachable() {
        if (dockerReachable != null) {
            return dockerReachable;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "info");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                dockerReachable = false;
                log.warn("[SandboxManager] Docker 探测超时");
                return false;
            }
            dockerReachable = process.exitValue() == 0;
            if (dockerReachable) {
                log.info("[SandboxManager] Docker 连接正常");
            } else {
                log.warn("[SandboxManager] Docker 连接失败 (exit code: {})", process.exitValue());
            }
        } catch (Exception e) {
            dockerReachable = false;
            log.warn("[SandboxManager] Docker 连接失败: {}", e.getMessage());
        }
        return dockerReachable;
    }

    @PreDestroy
    public void destroy() {
        activeSandboxes.forEach((key, sandbox) -> {
            try {
                sandbox.close();
            } catch (Exception e) {
                log.warn("[SandboxManager] 销毁沙箱失败 - key: {}", key, e);
            }
        });
        activeSandboxes.clear();
        log.info("[SandboxManager] 所有沙箱已销毁");
    }
}
