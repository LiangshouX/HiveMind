package com.liangshou.tangdynasty.agentic.agents.instance;

import com.liangshou.tangdynasty.agentic.agents.memory.longterm.ReMeLongTermMemoryAdapter;
import com.liangshou.tangdynasty.agentic.utils.SoulPromptLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.runtime.adapters.agentscope.AgentScopeAgentHandler;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 唐朝多 Agent 基础类
 * 提供通用的模型创建、状态加载、沙箱创建、长短期记忆注入等功能。
 */
public abstract class BaseTangDynastyAgent extends AgentScopeAgentHandler implements AgentInstanceFactory {

    private static final Logger logger = LoggerFactory.getLogger(BaseTangDynastyAgent.class);

    @Autowired(required = false)
    protected ReMeLongTermMemoryAdapter reMeLongTermMemoryAdapter;

    protected String getApiKey() {
        String dashscopeKey = System.getenv("DASHSCOPE_API_KEY");
        if (dashscopeKey == null || dashscopeKey.isEmpty()) {
            dashscopeKey = System.getenv("AI_DASHSCOPE_API_KEY");
        }
        if (dashscopeKey == null || dashscopeKey.isEmpty()) {
            throw new RuntimeException("DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable not set");
        }
        return dashscopeKey;
    }

    protected DashScopeChatModel createModel() {
        return DashScopeChatModel.builder()
                .apiKey(getApiKey())
                .modelName("qwen-plus")
                .stream(true)
                .enableThinking(true)
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(GenerateOptions.builder().thinkingBudget(1024).build())
                .build();
    }

    protected Sandbox createSandbox(String sessionId, String userId) {
        if (sandboxService == null) {
            return null;
        }
        try {
            return new BrowserSandbox(sandboxService, userId, sessionId);
        } catch (Exception e) {
            logger.warn("Failed to create sandbox: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 子类需实现如何构建包含工具的 ReActAgent 实例
     */
    public abstract ReActAgent buildAgent(String sessionId, String userId);

    @Override
    public String getSystemPrompt() {
        return SoulPromptLoader.loadSoul(getName());
    }

    @Override
    public Flux<Event> streamQuery(AgentRequest request, Object messages) {
        String sessionId = request.getSessionId();
        String userId = request.getUserId();

        try {
            // 1. Export state
            Map<String, Object> state = null;
            if (stateService != null) {
                try {
                    state = stateService.exportState(userId, sessionId, null).join();
                } catch (Exception e) {
                    logger.warn("Failed to export state: {}", e.getMessage());
                }
            }

            // 2. Build specific agent
            ReActAgent agent = buildAgent(sessionId, userId);

            // 3. Load state
            if (state != null && !state.isEmpty()) {
                try {
                    agent.loadStateDict(state);
                } catch (Exception e) {
                    logger.warn("Failed to load state: {}", e.getMessage());
                }
            }

            // 4. Handle messages
            List<Msg> agentMessages;
            if (messages instanceof List) {
                agentMessages = (List<Msg>) messages;
            } else if (messages instanceof Msg) {
                agentMessages = List.of((Msg) messages);
            } else {
                agentMessages = List.of();
            }

            Msg queryMessage;
            if (agentMessages.isEmpty()) {
                queryMessage = Msg.builder().role(io.agentscope.core.message.MsgRole.USER).build();
            } else if (agentMessages.size() == 1) {
                queryMessage = agentMessages.get(0);
            } else {
                for (int i = 0; i < agentMessages.size() - 1; i++) {
                    agent.getMemory().addMessage(agentMessages.get(i));
                }
                queryMessage = agentMessages.get(agentMessages.size() - 1);
            }

            StreamOptions streamOptions = StreamOptions.builder()
                    .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                    .incremental(true)
                    .build();

            // 5. Stream and save state
            return agent.stream(queryMessage, streamOptions)
                    .doFinally(signalType -> {
                        if (stateService != null) {
                            try {
                                Map<String, Object> finalState = agent.stateDict();
                                if (finalState != null && !finalState.isEmpty()) {
                                    stateService.saveState(userId, finalState, sessionId, null)
                                            .exceptionally(e -> {
                                                logger.error("Failed to save state: {}", e.getMessage());
                                                return null;
                                            });
                                }
                            } catch (Exception e) {
                                logger.error("Error saving state: {}", e.getMessage());
                            }
                        }
                    })
                    .doOnError(error -> logger.error("Error in agent stream: {}", error.getMessage()));

        } catch (Exception e) {
            logger.error("Error in streamQuery: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public void start() {
        logger.info("Starting {} Agent...", getName());
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing {} Agent...", getName());
    }
}
