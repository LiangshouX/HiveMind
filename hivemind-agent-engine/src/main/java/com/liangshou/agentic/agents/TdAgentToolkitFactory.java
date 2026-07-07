package com.liangshou.agentic.agents;

import com.liangshou.agentic.agents.guard.GuardedAgentTool;
import com.liangshou.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.sandbox.TdAgentSandboxManager;
import com.liangshou.agentic.agents.tools.SystemToolRegistry;
import com.liangshou.agentic.agents.tools.TdAgentBuiltinTools;
import com.liangshou.agentic.application.IToolConfigService;
import com.liangshou.agentic.application.dto.ToolConfigDTO;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.common.util.ToolConfigProvider;
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
    private final IConversationPersistenceService persistenceService;
    private final TdAgentSandboxManager sandboxManager;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;
    private final ToolConfigProvider toolConfigProvider;

    /**
     * 构造 Agent Toolkit 工厂实例。
     *
     * @param properties          Agent 外部化配置，包含沙箱、Tool Guard 等功能的开关和参数
     * @param persistenceService  对话持久化服务，用于内置工具的会话管理功能
     * @param sandboxManager      沙箱管理器，负责创建和管理代码执行沙箱环境
     * @param toolGuardEngine     Tool Guard 引擎，提供工具调用的安全防护和风险评估
     * @param toolApprovalService 工具审批服务，处理需要用户确认的高风险工具调用
     * @param toolConfigProvider  工具配置提供者，用于获取用户的工具启用状态
     */
    public TdAgentToolkitFactory(
            TdAgentProperties properties,
            IConversationPersistenceService persistenceService,
            TdAgentSandboxManager sandboxManager,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService,
            ToolConfigProvider toolConfigProvider) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.sandboxManager = sandboxManager;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
        this.toolConfigProvider = toolConfigProvider;
    }

    /**
     * 根据会话上下文创建并配置完整的 Toolkit 实例。
     *
     * <p>该方法按以下步骤构建 Toolkit：</p>
     * <ol>
     *     <li><b>注册内置工具</b>：添加会话管理、历史查询等基础工具（{@link TdAgentBuiltinTools}）</li>
     *     <li><b>检查沙箱配置</b>：如果沙箱未启用，直接返回仅包含内置工具的 Toolkit</li>
     *     <li><b>初始化沙箱环境</b>：创建 {@link BaseSandbox} 实例，绑定用户ID和会话ID</li>
     *     <li><b>注册代码执行工具</b>：Python 代码执行、Shell 命令执行</li>
     *     <li><b>注册文件系统工具</b>（如果启用）：读文件、写文件、列出目录、搜索文件</li>
     *     <li><b>注册浏览器工具</b>（如果启用）：导航、截图、点击、输入、等待、截屏</li>
     *     <li><b>应用 Tool Guard</b>：如果启用，将所有沙箱工具包装为 {@link GuardedAgentTool}</li>
     * </ol>
     *
     * <p><strong>沙箱初始化的容错处理：</strong></p>
     * <ul>
     *     <li>如果沙箱初始化失败且 {@code strictStartup=true}，抛出 {@link IllegalStateException}</li>
     *     <li>如果沙箱初始化失败且 {@code strictStartup=false}，记录警告日志并返回仅含内置工具的 Toolkit</li>
     * </ul>
     *
     * <p><strong>Tool Guard 集成：</strong></p>
     * <ul>
     *     <li>如果启用 Tool Guard，所有沙箱工具会被包装为 {@link GuardedAgentTool}</li>
     *     <li>包装后的工具会在执行前进行风险评估和用户审批检查</li>
     *     <li>内置工具不受 Tool Guard 影响，始终直接注册</li>
     * </ul>
     *
     * @param context 会话上下文，包含用户ID、会话ID等元数据，用于沙箱隔离和工具权限控制
     * @return 配置完成的 Toolkit 实例，包含所有可用的工具；若沙箱初始化失败则返回仅含内置工具的 Toolkit
     * @throws IllegalStateException 如果沙箱严格启动模式开启且初始化失败
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

    /**
     * 注册单个工具到 Toolkit，根据配置决定是否应用 Tool Guard 包装。
     *
     * <p>该方法根据用户的工具配置决定是否注册工具：</p>
     * <ul>
     *     <li><b>工具已禁用</b>：跳过注册，工具对 Agent 不可见</li>
     *     <li><b>启用 Tool Guard</b>：将工具包装为 {@link GuardedAgentTool}，在执行前进行风险评估和审批检查</li>
     *     <li><b>未启用 Tool Guard</b>：直接注册原始工具，无额外的安全检查</li>
     * </ul>
     *
     * <p><strong>适用场景：</strong></p>
     * <ul>
     *     <li>所有沙箱工具（Python、Shell、FileSystem、Browser）在注册时都会调用此方法</li>
     *     <li>内置工具不经过此方法，直接在 {@link #createToolkit} 中注册</li>
     * </ul>
     *
     * @param toolkit 目标 Toolkit 实例，工具将被注册到此对象
     * @param context 会话上下文，传递给 GuardedAgentTool 用于权限控制和审计日志
     * @param tool    要注册的原始工具实例
     */
    private void registerTool(Toolkit toolkit, ConversationSessionContext context, AgentTool tool) {
        String toolName = tool.getName();
        String userId = context.getUserId();

        // 获取用户工具配置
        ToolConfigDTO config = toolConfigProvider.getConfig(toolName, userId);

        // 如果禁用，跳过注册
        if (config != null && !config.getEnabled()) {
            log.debug("[Toolkit] 跳过已禁用工具: {}, userId: {}", toolName, userId);
            return;
        }

        // 如果启用 Tool Guard，包装
        if (properties.getToolGuard().isEnabled()) {
            toolkit.registerTool(new GuardedAgentTool(tool, context, toolGuardEngine, toolApprovalService));
            return;
        }

        toolkit.registerTool(tool);
    }
}
