package com.liangshou.agentic.agents;

import com.liangshou.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.hooks.StudioForwardMiddleware;
import com.liangshou.agentic.agents.hooks.TdAgentMemoryCompactionMiddleware;
import com.liangshou.agentic.agents.hooks.TdAgentToolGuardMiddleware;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.agentic.agents.memory.reme.ReMeMemoryTools;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.agents.observability.ObservabilityService;
import com.liangshou.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.agentic.agents.provider.TdAgentResolvedModelConfig;
import com.liangshou.agentic.agents.session.AgentSessionStateService;
import com.liangshou.agentic.agents.session.MongoAgentStateStore;
import com.liangshou.agentic.agents.skill.TdAgentSkillService;
import com.liangshou.agentic.agents.tools.TdAgentBuiltinTools;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.application.IConversationPersistenceService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.studio.StudioClient;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TdAgent 构建工厂 — 负责组装 {@link ReActAgent} 实例。
 *
 * <p>使用 AgentScope 2.0.0 的 MiddlewareBase 中间件体系替代已废弃的 Hook 机制。
 * 使用 AgentStateStore 替代已移除的 Memory/Session API。</p>
 */
@Component
public class TdAgentFactory {

    private static final Logger log = LoggerFactory.getLogger(TdAgentFactory.class);

    private final TdAgentProperties properties;
    private final TdAgentModelFactory modelFactory;
    private final TdAgentProviderRegistry providerRegistry;
    private final TdAgentToolkitFactory toolkitFactory;
    private final TdAgentPromptService promptService;
    private final IConversationPersistenceService persistenceService;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;
    private final TdAgentMemoryManager memoryManager;
    private final TdAgentReMeService reMeService;
    private final TdAgentSkillService skillService;
    private final AgentSessionStateService agentSessionStateService;
    private final ObservabilityService observabilityService;

    public TdAgentFactory(
            TdAgentProperties properties,
            TdAgentModelFactory modelFactory,
            TdAgentProviderRegistry providerRegistry,
            TdAgentToolkitFactory toolkitFactory,
            TdAgentPromptService promptService,
            IConversationPersistenceService persistenceService,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService,
            TdAgentMemoryManager memoryManager,
            TdAgentReMeService reMeService,
            TdAgentSkillService skillService,
            AgentSessionStateService agentSessionStateService,
            @Autowired(required = false) ObservabilityService observabilityService) {
        this.properties = properties;
        this.modelFactory = modelFactory;
        this.providerRegistry = providerRegistry;
        this.toolkitFactory = toolkitFactory;
        this.promptService = promptService;
        this.persistenceService = persistenceService;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
        this.memoryManager = memoryManager;
        this.reMeService = reMeService;
        this.skillService = skillService;
        this.agentSessionStateService = agentSessionStateService;
        this.observabilityService = observabilityService;
    }

    public ReActAgent createAgent(ConversationSessionContext context) {
        log.info("[TdAgent Factory] 开始创建 ReActAgent - userId: {}, sessionId: {}",
                context.getUserId(), context.getSessionId());

        // 1. 构建 System Prompt
        String systemPrompt = promptService.buildPrompt(context);
        log.debug("[TdAgent Factory] System Prompt 已构建 - length: {}", systemPrompt.length());

        // 2. 创建 MongoDB 记忆（辅助对象，用于压缩中间件）
        MongoConversationMemory memory =
                new MongoConversationMemory(context, persistenceService, systemPrompt);
        log.debug("[TdAgent Factory] MongoDB 记忆已初始化");

        // 3. 创建 Toolkit 并注册内置工具
        Toolkit toolkit = toolkitFactory.createToolkit(context);
        TdAgentBuiltinTools builtinTools = new TdAgentBuiltinTools(context, persistenceService);
        toolkit.registerTool(builtinTools);

        // 4. 注册 ReMe 记忆工具（HTTP 模式下直接注册 @Tool 工具）
        if (properties.getReme().isEnabled() && !reMeService.isUseMcp()) {
            toolkit.registerTool(new ReMeMemoryTools(reMeService, context));
            log.info("[TdAgent Factory] ReMe 记忆工具已注册 (HTTP 模式)");
        }
        log.debug("[TdAgent Factory] Toolkit 已创建");

        // 5. 创建 SkillBox
        SkillBox skillBox = skillService.createSkillBox(context, toolkit);
        log.debug("[TdAgent Factory] SkillBox 已创建 - isNull: {}", skillBox == null);

        // 6. 创建 AgentStateStore（替代已移除的 Session/Memory API）
        MongoAgentStateStore stateStore = agentSessionStateService.createStateStore();
        stateStore.prePopulateFromHistory(context, persistenceService);
        log.debug("[TdAgent Factory] AgentStateStore 已创建并预填充历史");

        // 7. 从 DB 解析模型配置
        TdAgentResolvedModelConfig resolvedConfig = context.getResolvedModelConfig();
        if (resolvedConfig == null) {
            log.info("[TdAgent Factory] 会话上下文无配置快照，从 DB 解析 - userId: {}, providerId: {}, modelId: {}",
                    context.getUserId(), context.getProviderId(), context.getModelId());
            resolvedConfig = providerRegistry.resolveForUser(
                    context.getUserId(), context.getProviderId(), context.getModelId());
        }

        // 8. 构建 ReActAgent（使用 2.0.0 中间件体系）
        var builder =
                ReActAgent.builder()
                        .name("HiveMindAgent")
                        .sysPrompt(systemPrompt)
                        .model(modelFactory.createFromConfig(resolvedConfig))
                        .toolkit(toolkit)
                        .stateStore(stateStore)
                        .defaultSessionId(context.getSessionId())
                        .maxIters(properties.getModel().getMaxIters());

        // 9. 添加记忆压缩中间件（替代已废弃的 TdAgentMemoryCompactionHook）
        builder.middleware(new TdAgentMemoryCompactionMiddleware(context, memory, memoryManager));

        // 10. 可选：Studio 可观测性中间件（替代已废弃的 StudioMessageHook）
        if (observabilityService != null && observabilityService.isAvailable()) {
            StudioClient client = observabilityService.getClient();
            builder.middleware(new StudioForwardMiddleware(client));
            log.info("[TdAgent Factory] 启用 Observability - studioClient: {}", client);
        } else {
            log.debug("[TdAgent Factory] Observability 未启用或不可用");
        }

        // 11. 可选：SkillBox（deprecated 但暂无非废弃替代方案可用）
        if (skillBox != null) {
            builder.skillBox(skillBox);
            log.debug("[TdAgent Factory] SkillBox 已添加到 Agent");
        }

        // 12. 可选：Tool Guard 中间件（替代已废弃的 TdAgentToolGuardHook）
        if (properties.getToolGuard().isEnabled()) {
            builder.middleware(new TdAgentToolGuardMiddleware(
                    context, toolGuardEngine, toolApprovalService));
            log.info("[TdAgent Factory] Tool Guard 中间件已启用");
        } else {
            log.debug("[TdAgent Factory] Tool Guard 未启用");
        }

        ReActAgent agent = builder.build();
        log.info("[TdAgent Factory] ReActAgent 创建完成 - name: {}, maxIters: {}",
                agent.getName(), properties.getModel().getMaxIters());
        return agent;
    }
}
