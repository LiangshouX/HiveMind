package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.tangdynasty.agentic.agents.hooks.TdAgentMemoryCompactionHook;
import com.liangshou.tangdynasty.agentic.agents.hooks.TdAgentToolGuardHook;
import com.liangshou.tangdynasty.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.tangdynasty.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.service.ConversationPersistenceService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.reme.ReMeLongTermMemory;
import org.springframework.stereotype.Component;

/**
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
            TdAgentReMeService reMeService) {
        this.properties = properties;
        this.modelFactory = modelFactory;
        this.toolkitFactory = toolkitFactory;
        this.promptService = promptService;
        this.persistenceService = persistenceService;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
        this.memoryManager = memoryManager;
        this.reMeService = reMeService;
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
        var builder =
                ReActAgent.builder()
                        .name("TDAgent")
                        .sysPrompt(systemPrompt)
                        .model(modelFactory.create())
                        .toolkit(toolkitFactory.createToolkit(context))
                        .memory(memory)
                        .hook(new TdAgentMemoryCompactionHook(context, memory, memoryManager))
                        .maxIters(properties.getModel().getMaxIters());
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

