package com.liangshou.tangdynasty.agentic.agents.sandbox;

import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/**
 * Sandbox 管理器 - 管理 AgentScope 沙箱服务的生命周期。
 *
 * <p>该组件提供以下功能：</p>
 * <ul>
 *     <li><b>懒加载初始化</b>：使用双重检查锁定模式，在首次请求时创建 SandboxService</li>
 *     <li><b>配置控制</b>：根据 {@link com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties.Sandbox} 决定是否启用沙箱</li>
 *     <li><b>资源清理</b>：在应用关闭时自动停止沙箱服务，释放资源</li>
 * </ul>
 *
 * <p>Sandbox 为 Agent 提供安全的代码执行环境，支持：</p>
 * <ul>
 *     <li>Shell 命令执行</li>
 *     <li>Python 代码运行</li>
 *     <li>文件系统操作</li>
 *     <li>浏览器自动化</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
public class TdAgentSandboxManager {

    private final TdAgentProperties properties;
    private volatile SandboxService sandboxService;

    /**
     * 执行相关操作。
     *
     * @param properties 外部化配置
     */
    public TdAgentSandboxManager(TdAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 执行 getSandboxService 操作。
     *
     * @return 返回结果
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
     * 执行 destroy 操作。
     */
    @PreDestroy
    public void destroy() {
        if (sandboxService != null) {
            sandboxService.close();
        }
    }
}

