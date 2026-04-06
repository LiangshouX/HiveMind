package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.tangdynasty.agentic.agents.hooks.TdAgentMemoryCompactionHook;
import com.liangshou.tangdynasty.agentic.agents.hooks.TdAgentToolGuardHook;
import com.liangshou.tangdynasty.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.tangdynasty.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.tangdynasty.agentic.agents.skill.TdAgentSkillService;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.reme.ReMeLongTermMemory;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
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

    private final TdAgentProperties properties;
    private final TdAgentModelFactory modelFactory;
    private final TdAgentToolkitFactory toolkitFactory;
    private final TdAgentPromptService promptService;
    private final ConversationPersistenceService persistenceService;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;
    private final TdAgentMemoryManager memoryManager;
    private final TdAgentReMeService reMeService;
    private final TdAgentSkillService skillService;

    /**
     * 执行相关操作。
     * @param properties 外部化配置
     * @param modelFactory 模型工厂
     * @param toolkitFactory toolkit 工厂
     * @param promptService prompt 服务
     * @param persistenceService 持久化服务
     * @param toolGuardEngine tool guard 引擎
     * @param toolApprovalService 工具审批服务
     * @param memoryManager 记忆管理器
     * @param reMeService ReMe 集成服务
     * @param skillService skill 服务
     */
    public TdAgentFactory(
            TdAgentProperties properties,
            TdAgentModelFactory modelFactory,
            TdAgentToolkitFactory toolkitFactory,
            TdAgentPromptService promptService,
            ConversationPersistenceService persistenceService,
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
     * 创建 Agent 实例。
     * @param context 会话上下文
     * @return 返回结果
     */
    public ReActAgent createAgent(ConversationSessionContext context) {
        String systemPrompt = promptService.buildPrompt(context);
        MongoConversationMemory memory =
                new MongoConversationMemory(context, persistenceService, systemPrompt);
        Toolkit toolkit = toolkitFactory.createToolkit(context);
        SkillBox skillBox = skillService.createSkillBox(context, toolkit);
        var builder =
                ReActAgent.builder()
                        .name("TDAgent")
                        .sysPrompt(systemPrompt)
                        .model(modelFactory.create())
                        .toolkit(toolkit)
                        .memory(memory)
                        .hook(new TdAgentMemoryCompactionHook(context, memory, memoryManager))
                        .maxIters(properties.getModel().getMaxIters());
        if (skillBox != null) {
            builder.skillBox(skillBox);
        }
        if (properties.getToolGuard().isEnabled()) {
            builder.hook(new TdAgentToolGuardHook(context, toolGuardEngine, toolApprovalService));
        }
        if (properties.getReme().isEnabled()) {
            builder.longTermMemory(
                            ReMeLongTermMemory.builder()
                                    .userId(reMeService.userWorkspaceId(context))
                                    .apiBaseUrl(properties.getReme().getBaseUrl())
                                    .build())
                    .longTermMemoryMode(LongTermMemoryMode.BOTH);
        }
        return builder.build();
    }
}
