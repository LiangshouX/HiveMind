package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.agents.guard.GuardedAgentTool;
import com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.tangdynasty.agentic.agents.sandbox.TdAgentSandboxManager;
import com.liangshou.tangdynasty.agentic.agents.tools.TdAgentBuiltinTools;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agent Toolkit 工厂类，负责创建和配置 Agent 的工具集（Toolkit）。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>注册内置工具（如会话管理、历史查询等）</li>
 *   <li>根据配置动态注册沙箱工具，包括：</li>
 *   <ul>
 *     <li>Python 代码执行工具</li>
 *     <li>Shell 命令执行工具</li>
 *     <li>文件系统操作工具（读/写文件、列出目录、搜索文件）</li>
 *     <li>浏览器自动化工具（导航、截图、点击、输入、等待等）</li>
 *   </ul>
 *   <li>集成 Tool Guard 引擎，为工具调用添加安全防护和审批机制</li>
 *   <li>支持沙箱初始化失败时的优雅降级处理</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Component
public class TdAgentToolkitFactory {

    private static final Logger log = LoggerFactory.getLogger(TdAgentToolkitFactory.class);

    private final TdAgentProperties properties;
    private final ConversationPersistenceService persistenceService;
    private final TdAgentSandboxManager sandboxManager;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;

    /**
     * 执行相关操作。
     * @param properties 外部化配置
     * @param persistenceService 持久化服务
     * @param sandboxManager 沙箱管理器
     * @param toolGuardEngine tool guard 引擎
     * @param toolApprovalService 工具审批服务
     */
    public TdAgentToolkitFactory(
            TdAgentProperties properties,
            ConversationPersistenceService persistenceService,
            TdAgentSandboxManager sandboxManager,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.sandboxManager = sandboxManager;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
    }

    /**
     * 创建 toolkit 实例。
     * @param context 会话上下文
     * @return 返回结果
     */
    public Toolkit createToolkit(ConversationSessionContext context) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new TdAgentBuiltinTools(context, persistenceService));
        if (!properties.getSandbox().isEnabled()) {
            return toolkit;
        }
        try {
            SandboxService sandboxService = sandboxManager.getSandboxService();
            BaseSandbox baseSandbox = new BaseSandbox(sandboxService, context.getUserId(), context.getSessionId());
            registerTool(toolkit, context, ToolkitInit.RunPythonCodeTool(baseSandbox));
            registerTool(toolkit, context, ToolkitInit.RunShellCommandTool(baseSandbox));
            if (properties.getSandbox().isFilesystemEnabled()) {
                registerTool(toolkit, context, ToolkitInit.ReadFileTool(baseSandbox));
                registerTool(toolkit, context, ToolkitInit.WriteFileTool(baseSandbox));
                registerTool(toolkit, context, ToolkitInit.ListDirectoryTool(baseSandbox));
                registerTool(toolkit, context, ToolkitInit.SearchFilesTool(baseSandbox));
            }
            if (properties.getSandbox().isBrowserEnabled()) {
                BrowserSandbox browserSandbox =
                        new BrowserSandbox(sandboxService, context.getUserId(), context.getSessionId());
                registerTool(toolkit, context, ToolkitInit.BrowserNavigateTool(browserSandbox));
                registerTool(toolkit, context, ToolkitInit.BrowserSnapshotTool(browserSandbox));
                registerTool(toolkit, context, ToolkitInit.BrowserClickTool(browserSandbox));
                registerTool(toolkit, context, ToolkitInit.BrowserTypeTool(browserSandbox));
                registerTool(toolkit, context, ToolkitInit.BrowserWaitForTool(browserSandbox));
                registerTool(toolkit, context, ToolkitInit.BrowserTakeScreenshotTool(browserSandbox));
            }
        } catch (Exception ex) {
            if (properties.getSandbox().isStrictStartup()) {
                throw new IllegalStateException("Sandbox 工具初始化失败。", ex);
            }
            log.warn("Sandbox 工具初始化失败，当前请求将退化为内置工具模式: {}", ex.getMessage());
        }
        return toolkit;
    }

    private void registerTool(Toolkit toolkit, ConversationSessionContext context, AgentTool tool) {
        if (properties.getToolGuard().isEnabled()) {
            toolkit.registerTool(new GuardedAgentTool(tool, context, toolGuardEngine, toolApprovalService));
            return;
        }
        toolkit.registerTool(tool);
    }
}
