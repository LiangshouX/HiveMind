package com.liangshou.agentic.agents.sandbox;

import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sandbox 管理器 - 管理 AgentScope 沙箱服务的生命周期。
 *
 * <p>该组件提供以下功能：</p>
 * <ul>
 *     <li><b>懒加载初始化</b>：使用双重检查锁定模式，在首次请求时创建 SandboxService</li>
 *     <li><b>配置控制</b>：根据 {@link com.liangshou.agentic.common.config.TdAgentProperties.Sandbox} 决定是否启用沙箱</li>
 *     <li><b>Docker 连接探测</b>：检测 Docker 容器运行时是否可达</li>
 *     <li><b>资源清理</b>：在应用关闭时自动停止沙箱服务，释放资源</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
public class TdAgentSandboxManager {

    private static final Logger log = LoggerFactory.getLogger(TdAgentSandboxManager.class);

    private final TdAgentProperties properties;
    private volatile SandboxService sandboxService;
    private volatile Boolean dockerReachable;

    public TdAgentSandboxManager(TdAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取沙箱服务（懒加载）。
     */
    public SandboxService getSandboxService() {
        if (!properties.getSandbox().isEnabled()) {
            return null;
        }
        if (sandboxService == null) {
            synchronized (this) {
                if (sandboxService == null) {
                    SandboxService created = new SandboxService(ManagerConfig.builder().build());
                    created.start();
                    sandboxService = created;
                }
            }
        }
        return sandboxService;
    }

    /**
     * 检测 Docker 容器运行时是否可连接。
     *
     * <p>通过执行 {@code docker info} 命令来验证 Docker daemon 是否可达。
     * 结果会被缓存，避免重复探测。</p>
     *
     * @return true 如果 Docker 可达，false 否则
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
        if (sandboxService != null) {
            sandboxService.close();
        }
    }
}

