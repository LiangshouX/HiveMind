package com.liangshou.agentic.agents;

import com.liangshou.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.hooks.TdAgentMemoryCompactionHook;
import com.liangshou.agentic.agents.hooks.TdAgentToolGuardHook;
import com.liangshou.agentic.agents.memory.MongoConversationMemory;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.agents.skill.TdAgentSkillService;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.application.IConversationPersistenceService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.reme.ReMeLongTermMemory;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.studio.StudioClient;
import io.agentscope.core.studio.StudioManager;
import io.agentscope.core.studio.StudioMessageHook;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ReAct Agent 工厂类，负责创建和配置完整的 Agent 实例。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>构建 system prompt 并创建 MongoDB 会话记忆</li>
 *   <li>创建 Toolkit 并注册所有可用工具</li>
 *   <li>创建 SkillBox 并激活用户启用的 Skills</li>
 *   <li>配置记忆压缩 Hook，自动管理长对话的上下文</li>
 *   <li>配置 Tool Guard Hook，实现工具调用的安全防护和审批</li>
 *   <li>可选集成 ReMe 长期记忆服务，增强 Agent 的记忆能力</li>
 *   <li>设置最大迭代次数等运行时参数</li>
 * </ul>
 * </p>
 * <p>
 * 该工厂整合了所有 Agent 核心组件，包括模型、工具、记忆、Skills、Hooks 等，
 * 是创建可运行 Agent 实例的统一入口。
 * </p>
 *
 * @author LiangshouX
 */
@Component
public class TdAgentFactory {

    private static final Logger log = LoggerFactory.getLogger(TdAgentFactory.class);

    private final TdAgentProperties properties;
    private final TdAgentModelFactory modelFactory;
    private final TdAgentToolkitFactory toolkitFactory;
    private final TdAgentPromptService promptService;
    private final IConversationPersistenceService persistenceService;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;
    private final TdAgentMemoryManager memoryManager;
    private final TdAgentReMeService reMeService;
    private final TdAgentSkillService skillService;

    /**
     * 构造 ReAct Agent 工厂实例。
     *
     * @param properties          Agent 外部化配置，包含模型参数、沙箱设置、Tool Guard 开关等
     * @param modelFactory        模型工厂，负责创建 LLM 模型实例
     * @param toolkitFactory      Toolkit 工厂，负责创建和配置工具集
     * @param promptService       Prompt 服务，负责构建系统提示词
     * @param persistenceService  对话持久化服务，用于会话历史的读写操作
     * @param toolGuardEngine     Tool Guard 引擎，提供工具调用的安全防护
     * @param toolApprovalService 工具审批服务，处理需要用户确认的高风险操作
     * @param memoryManager       记忆管理器，负责对话记忆的压缩和管理
     * @param reMeService         ReMe 集成服务，提供长期记忆能力
     * @param skillService        Skill 服务，管理用户的自定义 Skills
     */
    public TdAgentFactory(
            TdAgentProperties properties,
            TdAgentModelFactory modelFactory,
            TdAgentToolkitFactory toolkitFactory,
            TdAgentPromptService promptService,
            IConversationPersistenceService persistenceService,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService,
            TdAgentMemoryManager memoryManager,
            TdAgentReMeService reMeService,
            TdAgentSkillService skillService) {
        this.properties = properties;
        this.modelFactory = modelFactory;
        this.toolkitFactory = toolkitFactory;
        this.promptService = promptService;
        this.persistenceService = persistenceService;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
        this.memoryManager = memoryManager;
        this.reMeService = reMeService;
        this.skillService = skillService;
    }

    /**
     * 根据会话上下文创建并配置完整的 ReAct Agent 实例。
     *
     * <p>该方法按以下步骤构建 Agent：</p>
     * <ol>
     *     <li><b>构建 System Prompt</b>：调用 {@link TdAgentPromptService#buildPrompt} 生成个性化提示词</li>
     *     <li><b>创建 MongoDB 记忆</b>：初始化 {@link MongoConversationMemory}，加载历史对话数据</li>
     *     <li><b>创建 Toolkit</b>：调用 {@link TdAgentToolkitFactory#createToolkit} 注册所有可用工具</li>
     *     <li><b>创建 SkillBox</b>：调用 {@link TdAgentSkillService#createSkillBox} 激活用户启用的 Skills</li>
     *     <li><b>配置基础组件</b>：设置名称、提示词、模型、工具集、记忆等核心组件</li>
     *     <li><b>添加记忆压缩 Hook</b>：注册 {@link TdAgentMemoryCompactionHook} 自动管理长对话上下文</li>
     *     <li><b>设置最大迭代次数</b>：从配置中读取 {@code maxIters} 限制 Agent 的思考步数</li>
     *     <li><b>可选添加 SkillBox</b>：如果 SkillBox 不为 null，则添加到 Agent</li>
     *     <li><b>可选添加 Tool Guard Hook</b>：如果启用 Tool Guard，注册 {@link TdAgentToolGuardHook}</li>
     *     <li><b>可选集成 ReMe 长期记忆</b>：如果启用 ReMe，配置长期记忆服务和模式</li>
     * </ol>
     *
     * <p><strong>核心组件说明：</strong></p>
     * <ul>
     *     <li><b>记忆压缩 Hook</b>：在对话消息过多时自动触发压缩，保留最近消息并生成摘要</li>
     *     <li><b>Tool Guard Hook</b>：拦截工具调用，进行风险评估和用户审批检查</li>
     *     <li><b>ReMe 长期记忆</b>：跨会话持久化关键信息，增强 Agent 的长期记忆能力</li>
     * </ul>
     *
     * <p><strong>配置依赖：</strong></p>
     * <ul>
     *     <li>{@code properties.getToolGuard().isEnabled()}：决定是否启用 Tool Guard Hook</li>
     *     <li>{@code properties.getReme().isEnabled()}：决定是否集成 ReMe 长期记忆</li>
     *     <li>{@code properties.getModel().getMaxIters()}：设置 Agent 的最大迭代次数</li>
     * </ul>
     *
     * @param context 会话上下文，包含用户ID、会话ID、会话标题等元数据，用于初始化记忆和工具权限
     * @return 配置完成的 ReActAgent 实例，已准备好接收用户消息并执行任务
     */
    public ReActAgent createAgent(ConversationSessionContext context) {
        log.info("[TdAgent Factory] 开始创建 ReActAgent - userId: {}, sessionId: {}",
                context.getUserId(), context.getSessionId());

        String systemPrompt = promptService.buildPrompt(context);
        log.debug("[TdAgent Factory] System Prompt 已构建 - length: {}", systemPrompt.length());

        MongoConversationMemory memory =
                new MongoConversationMemory(context, persistenceService, systemPrompt);
        log.debug("[TdAgent Factory] MongoDB 记忆已初始化");

        Toolkit toolkit = toolkitFactory.createToolkit(context);
        log.debug("[TdAgent Factory] Toolkit 已创建");

        SkillBox skillBox = skillService.createSkillBox(context, toolkit);
        log.debug("[TdAgent Factory] SkillBox 已创建 - isNull: {}", skillBox == null);

        log.info("[TdAgent Factory] 开始构建 ReActAgent 实例");
        var builder =
                ReActAgent.builder()
                        .name("HiveMindAgent")
                        .sysPrompt(systemPrompt)
                        .model(modelFactory.create(context.getProviderId(), context.getModelId()))
                        .toolkit(toolkit)
                        .memory(memory)
                        .hook(new TdAgentMemoryCompactionHook(context, memory, memoryManager))
                        .maxIters(properties.getModel().getMaxIters());

        if (properties.getObservability() != null && properties.getObservability().isEnabled()) {
            // 初始化 Studio 连接
            StudioManager.init()
                    .studioUrl(properties.getObservability().getUrl() != null ?
                            properties.getObservability().getUrl() : "http://localhost:5173")
                    .project(properties.getSystemPrompt().getProductName())
                    .runName("%s-%d".formatted(
                            properties.getSystemPrompt().getProductName(),
                            System.currentTimeMillis())
                    )
                    .initialize()
                    .block();

            StudioClient studioClient = StudioManager.getClient();
            log.info("[TdAgent Factory] 启用 Observability - studioClient: {}", studioClient);
            builder.hook(new StudioMessageHook(studioClient));
        }

        if (skillBox != null) {
            builder.skillBox(skillBox);
            log.debug("[TdAgent Factory] SkillBox 已添加到 Agent");
        }

        if (properties.getToolGuard().isEnabled()) {
            builder.hook(new TdAgentToolGuardHook(context, toolGuardEngine, toolApprovalService));
            log.info("[TdAgent Factory] Tool Guard Hook 已启用");
        } else {
            log.debug("[TdAgent Factory] Tool Guard 未启用");
        }

        if (properties.getReme().isEnabled()) {
            builder.longTermMemory(
                            ReMeLongTermMemory.builder()
                                    .userId(reMeService.userWorkspaceId(context))
                                    .apiBaseUrl(properties.getReme().getBaseUrl())
                                    .build())
                    .longTermMemoryMode(LongTermMemoryMode.BOTH);
            log.info("[TdAgent Factory] ReMe 长期记忆已启用");
        } else {
            log.debug("[TdAgent Factory] ReMe 长期记忆未启用");
        }

        ReActAgent agent = builder.build();
        log.info("[TdAgent Factory] ReActAgent 创建完成 - name: {}, maxIters: {}",
                agent.getName(), properties.getModel().getMaxIters());
        return agent;
    }
}
