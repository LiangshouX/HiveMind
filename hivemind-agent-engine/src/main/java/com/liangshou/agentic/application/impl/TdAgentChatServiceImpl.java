package com.liangshou.agentic.application.impl;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.TdAgentFactory;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.agentic.agents.provider.TdAgentResolvedModelConfig;
import com.liangshou.agentic.agents.session.AgentSessionStateService;
import com.liangshou.agentic.domain.memory.model.ConversationMemoryDocument;
import com.liangshou.agentic.domain.memory.model.ConversationViewDocument;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.application.IChatCommandService;
import com.liangshou.agentic.application.ITdAgentChatService;
import com.liangshou.agentic.application.dto.ChatRequest;
import com.liangshou.agentic.application.dto.ChatResponse;
import com.liangshou.agentic.application.dto.SessionHistoryResponse;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Agent 聊天服务核心组件，负责处理用户与 AI Agent 之间的对话交互。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>处理同步聊天请求，创建和管理 ReAct Agent 实例</li>
 *   <li>管理会话上下文，包括用户 ID、会话 ID 和会话标题</li>
 *   <li>集成命令服务，支持特殊命令（如 /clear、/history、/new）的处理</li>
 *   <li>持久化会话历史，保存和检索对话消息</li>
 *   <li>管理会话状态，支持 Agent 状态的恢复和保存</li>
 *   <li>处理工具调用审批流程，检测并返回待审批的工具调用</li>
 *   <li>提供会话列表查询和历史记录检索功能</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Service
@SuppressWarnings("unused")
public class TdAgentChatServiceImpl implements ITdAgentChatService {

    private final TdAgentFactory agentFactory;
    private final IChatCommandService chatCommandService;
    private final IConversationPersistenceService persistenceService;
    private final AgentSessionStateService agentSessionStateService;
    private final ToolApprovalService toolApprovalService;
    private final TdAgentProviderRegistry providerRegistry;

    /**
     * 构造器
     *
     * @param agentFactory             Agent 工厂
     * @param chatCommandService       命令服务
     * @param persistenceService       持久化服务
     * @param agentSessionStateService 会话状态服务
     * @param toolApprovalService      工具审批服务
     * @param providerRegistry         供应商注册表
     */
    public TdAgentChatServiceImpl(
            TdAgentFactory agentFactory,
            IChatCommandService chatCommandService,
            IConversationPersistenceService persistenceService,
            AgentSessionStateService agentSessionStateService,
            ToolApprovalService toolApprovalService,
            TdAgentProviderRegistry providerRegistry) {
        this.agentFactory = agentFactory;
        this.chatCommandService = chatCommandService;
        this.persistenceService = persistenceService;
        this.agentSessionStateService = agentSessionStateService;
        this.toolApprovalService = toolApprovalService;
        this.providerRegistry = providerRegistry;
    }

    /**
     * 处理聊天请求。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    @Override
    public ChatResponse chat(ChatRequest request) {
        ConversationSessionContext context = buildContext(request);
        ChatResponse commandResponse = chatCommandService.handleCommand(context, request.getMessage());
        if (commandResponse != null) {
            return commandResponse;
        }
        ReActAgent agent = agentFactory.createAgent(context);
        agentSessionStateService.restore(context, agent);
        Msg userMessage = Msg.builder()
                .name(request.getUserName() == null ? "user" : request.getUserName())
                .role(MsgRole.USER)
                .textContent(request.getMessage())
                .build();
        Msg response = agent.call(userMessage).block();
        boolean paused = isPaused(response);
        agentSessionStateService.save(context, agent, paused);
        long messageCount = persistenceService.countMessages(context);
        ConversationViewDocument sessionView =
                persistenceService
                        .getSessionView(context.getUserId(), context.getSessionId())
                        .orElse(null);
        return ChatResponse.builder()
                .success(true)
                .commandHandled(false)
                .userId(context.getUserId())
                .sessionId(context.getSessionId())
                .title(sessionView == null ? context.getSessionTitle() : sessionView.getTitle())
                .message(response == null ? "" : response.getTextContent())
                .messageCount(messageCount)
                .timestamp(Instant.now().toString())
                .metadata(
                        Map.of(
                                "agentName", agent.getName(),
                                "generateReason",
                                response == null || response.getGenerateReason() == null
                                        ? ""
                                        : response.getGenerateReason().name(),
                                "paused", paused,
                                "pendingApprovals",
                                paused
                                        ? toolApprovalService.listPending(
                                        context.getUserId(), context.getSessionId())
                                        : List.of()))
                .build();
    }

    /**
     * 列出会话列表。
     *
     * @param userId 用户标识
     * @return 返回结果
     */
    @Override
    public List<ConversationViewDocument> listSessions(String userId) {
        return persistenceService.listSessions(userId);
    }

    /**
     * 获取会话历史。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    @Override
    public SessionHistoryResponse getSessionHistory(String userId, String sessionId) {
        ConversationViewDocument view =
                persistenceService.getSessionView(userId, sessionId).orElse(null);
        ConversationMemoryDocument history =
                persistenceService.getSessionHistory(userId, sessionId).orElse(null);
        return SessionHistoryResponse.builder()
                .session(view)
                .messages(history == null ? List.of() : history.getMessages())
                .build();
    }

    @Override
    public void deleteSession(String userId, String sessionId) {
        persistenceService.deleteSession(userId, sessionId);
    }

    @Override
    public ConversationSessionContext buildContext(ChatRequest request) {
        // 从 DB 解析模型配置并绑定到会话上下文（会话级快照）
        TdAgentResolvedModelConfig resolvedConfig = providerRegistry.resolveForUser(
                request.getUserId(), request.getProviderId(), request.getModelId());

        return ConversationSessionContext.builder()
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .userName(request.getUserName())
                .sessionTitle(request.getTitle())
                .modelId(request.getModelId())
                .providerId(request.getProviderId())
                .resolvedModelConfig(resolvedConfig)
                .build();
    }

    /**
     * 构建会话上下文。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @param title     会话标题
     * @return 返回结果
     */
    @Override
    public ConversationSessionContext buildContext(String userId, String sessionId, String title) {
        // 审批/拒绝恢复场景：从会话状态恢复原始供应商，保证会话级配置一致性
        ConversationSessionContext base = ConversationSessionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .sessionTitle(title)
                .build();

        String storedProviderId = agentSessionStateService.getStoredProviderId(base);
        String storedModelId = agentSessionStateService.getStoredModelId(base);

        TdAgentResolvedModelConfig resolvedConfig;
        if (storedProviderId != null) {
            // 优先使用原始供应商（会话连续性），若已被删除则降级到激活供应商
            resolvedConfig = tryResolveWithFallback(userId, storedProviderId, storedModelId);
        } else {
            // 旧会话无持久化供应商记录，使用激活供应商
            resolvedConfig = providerRegistry.resolveForUser(userId, null, null);
        }

        return ConversationSessionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .sessionTitle(title)
                .providerId(resolvedConfig.getProviderId())
                .modelId(resolvedConfig.getModelId())
                .resolvedModelConfig(resolvedConfig)
                .build();
    }

    /**
     * 尝试用指定供应商解析配置，若供应商已被删除则降级到激活供应商。
     */
    private TdAgentResolvedModelConfig tryResolveWithFallback(
            String userId, String providerId, String modelId) {
        try {
            return providerRegistry.resolveForUser(userId, providerId, modelId);
        } catch (IllegalStateException e) {
            // 供应商已被删除，降级到激活供应商
            return providerRegistry.resolveForUser(userId, null, null);
        }
    }

}
