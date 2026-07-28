package com.liangshou.agentic.agents;

import com.liangshou.agentic.agents.guard.GuardedAgentTool;
import com.liangshou.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.memory.reme.McpReMeClient;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.agents.sandbox.TdAgentSandboxManager;
import com.liangshou.agentic.agents.tools.TdAgentBuiltinTools;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.application.dto.ToolConfigDTO;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import com.liangshou.agentic.common.util.ToolConfigProvider;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpTool;
import io.agentscope.harness.agent.filesystem.sandbox.SandboxBackedFilesystem;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.tool.FilesystemTool;
import io.agentscope.harness.agent.tool.ShellExecuteTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent Toolkit 工厂类，负责创建和配置 Agent 的工具集（Toolkit）。
 *
 * <p>基于 AgentScope 2.0 harness 模块注册沙箱工具：</p>
 * <ul>
 *   <li>Shell 命令执行工具（{@link ShellExecuteTool}）</li>
 *   <li>文件系统工具（{@link FilesystemTool}）：读文件、写文件、编辑文件、搜索文件、列出目录</li>
 * </ul>
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
    private final TdAgentReMeService remeService;

    public TdAgentToolkitFactory(
            TdAgentProperties properties,
            IConversationPersistenceService persistenceService,
            TdAgentSandboxManager sandboxManager,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService,
            ToolConfigProvider toolConfigProvider,
            TdAgentReMeService remeService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.sandboxManager = sandboxManager;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
        this.toolConfigProvider = toolConfigProvider;
        this.remeService = remeService;
    }

    /**
     * 根据会话上下文创建并配置完整的 Toolkit 实例。
     */
    public Toolkit createToolkit(ConversationSessionContext context) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new TdAgentBuiltinTools(context, persistenceService));

        // 注册 ReMe MCP 记忆工具（如果 MCP 模式已启用）
        registerReMeMcpTools(toolkit, context);

        // 注册沙箱工具（如果沙箱已启用）
        if (properties.getSandbox().isEnabled()) {
            registerSandboxTools(toolkit, context);
        }

        return toolkit;
    }

    /**
     * 注册沙箱工具到 Toolkit。
     *
     * <p>基于 AgentScope 2.0 harness 模块：</p>
     * <ol>
     *   <li>通过 {@link TdAgentSandboxManager} 获取或创建 Docker 沙箱</li>
     *   <li>创建 {@link SandboxBackedFilesystem} 包装沙箱</li>
     *   <li>注册 {@link ShellExecuteTool}（Shell 命令执行）</li>
     *   <li>注册 {@link FilesystemTool}（文件读写、编辑、搜索）</li>
     * </ol>
     */
    private void registerSandboxTools(Toolkit toolkit, ConversationSessionContext context) {
        try {
            Sandbox sandbox = sandboxManager.getOrCreateSandbox(
                    context.getUserId(), context.getSessionId());
            if (sandbox == null) {
                log.warn("[Toolkit] 沙箱创建失败，跳过沙箱工具注册");
                return;
            }

            // 创建 SandboxBackedFilesystem 作为工具的底层执行器
            SandboxBackedFilesystem filesystem = new SandboxBackedFilesystem();
            filesystem.setSandbox(sandbox);

            // 注册 Shell 命令执行工具
            ShellExecuteTool shellTool = new ShellExecuteTool(filesystem);
            registerTool(toolkit, context, shellTool);

            // 注册文件系统工具（如果启用）
            if (properties.getSandbox().isFilesystemEnabled()) {
                FilesystemTool fsTool = new FilesystemTool(filesystem);
                registerTool(toolkit, context, fsTool);
            }

            log.info("[Toolkit] 沙箱工具注册完成 - userId: {}, sessionId: {}",
                    context.getUserId(), context.getSessionId());
        } catch (Exception ex) {
            if (properties.getSandbox().isStrictStartup()) {
                throw new BizException(HmeErrorCode.AGENT_SANDBOX_INIT_ERROR, ex);
            }
            log.warn("[Toolkit] 沙箱工具初始化失败，退化为内置工具模式: {}", ex.getMessage());
        }
    }

    /**
     * 注册单个工具到 Toolkit，根据配置决定是否应用 Tool Guard 包装。
     *
     * <p>支持两种工具类型：</p>
     * <ul>
     *     <li>{@link AgentTool} — 实现 AgentTool 接口的工具</li>
     *     <li>使用 {@code @Tool} 注解的对象 — 通过 Toolkit.registerTool(Object) 注册</li>
     * </ul>
     */
    private void registerTool(Toolkit toolkit, ConversationSessionContext context, Object tool) {
        String userId = context.getUserId();
        List<String> toolNames = resolveToolNames(tool);

        // 按真实工具名检查启用配置。
        // 注意：@Tool 注解对象由框架按"注解名/方法名"注册，不能用类名查配置（否则禁用配置永不生效）
        List<String> enabledNames = new ArrayList<>();
        for (String toolName : toolNames) {
            ToolConfigDTO config = toolConfigProvider.getConfig(toolName, userId);
            if (config != null && !config.getEnabled()) {
                log.debug("[Toolkit] 跳过已禁用工具: {}, userId: {}", toolName, userId);
            } else {
                enabledNames.add(toolName);
            }
        }
        if (enabledNames.isEmpty()) {
            return;
        }
        if (enabledNames.size() < toolNames.size()) {
            // Toolkit.registerTool(Object) 会整体注册对象的全部 @Tool 方法，暂不支持按方法粒度剔除
            log.warn("[Toolkit] 工具对象 {} 含被禁用工具 {}，框架仅支持整体注册，本次仍整体注册",
                    tool.getClass().getSimpleName(),
                    toolNames.stream().filter(n -> !enabledNames.contains(n)).toList());
        }

        // AgentTool 接口的工具支持 Tool Guard 包装
        if (properties.getToolGuard().isEnabled() && tool instanceof AgentTool agentTool) {
            toolkit.registerTool(new GuardedAgentTool(agentTool, context, toolGuardEngine, toolApprovalService));
            return;
        }

        toolkit.registerTool(tool);
    }

    /**
     * 解析工具对象暴露的真实工具名，与框架 {@code Toolkit.registerTool(Object)} 的注册逻辑保持一致：
     * {@link AgentTool} 取 {@code getName()}；{@code @Tool} 注解对象按方法取注解名（缺省为方法名）。
     */
    private List<String> resolveToolNames(Object tool) {
        if (tool instanceof AgentTool agentTool) {
            return List.of(agentTool.getName());
        }
        List<String> names = new ArrayList<>();
        for (java.lang.reflect.Method method : tool.getClass().getDeclaredMethods()) {
            Tool annotation = method.getAnnotation(Tool.class);
            if (annotation != null) {
                names.add(annotation.name().isEmpty() ? method.getName() : annotation.name());
            }
        }
        return names;
    }

    /**
     * 注册 ReMe MCP 记忆工具到 Toolkit。
     */
    private void registerReMeMcpTools(Toolkit toolkit, ConversationSessionContext context) {
        if (!remeService.isUseMcp()) {
            return;
        }
        McpReMeClient mcpClient = remeService.getMcpReMeClient();
        if (mcpClient == null || !mcpClient.isInitialized()) {
            log.warn("[Toolkit] MCP 客户端未初始化，跳过 ReMe MCP 工具注册");
            return;
        }
        try {
            String userId = context.getUserId();
            java.util.List<McpTool> mcpTools = mcpClient.toAgentTools(userId);
            for (McpTool mcpTool : mcpTools) {
                registerTool(toolkit, context, mcpTool);
            }
            log.info("[Toolkit] 已注册 {} 个 ReMe MCP 记忆工具", mcpTools.size());
        } catch (Exception e) {
            log.error("[Toolkit] 注册 ReMe MCP 工具失败", e);
        }
    }
}
