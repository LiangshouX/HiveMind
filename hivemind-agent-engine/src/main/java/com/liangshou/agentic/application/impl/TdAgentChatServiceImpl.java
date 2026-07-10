package com.liangshou.agentic.application.impl;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.TdAgentFactory;
import com.liangshou.agentic.agents.TdAgentModelFactory;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.agentic.agents.provider.TdAgentResolvedModelConfig;
import com.liangshou.agentic.agents.session.AgentSessionStateService;
import com.liangshou.agentic.domain.memory.model.ConversationMemoryDocument;
import com.liangshou.agentic.domain.memory.model.ConversationViewDocument;
import com.liangshou.agentic.domain.memory.model.StoredMessage;
import com.liangshou.agentic.domain.memory.model.StoredMessageContent;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.application.IChatCommandService;
import com.liangshou.agentic.application.ITdAgentChatService;
import com.liangshou.agentic.application.dto.ChatRequest;
import com.liangshou.agentic.application.dto.ChatResponse;
import com.liangshou.agentic.application.dto.SessionHistoryResponse;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TdAgentChatServiceImpl.class);

    private final TdAgentFactory agentFactory;
    private final IChatCommandService chatCommandService;
    private final IConversationPersistenceService persistenceService;
    private final AgentSessionStateService agentSessionStateService;
    private final ToolApprovalService toolApprovalService;
    private final TdAgentProviderRegistry providerRegistry;
    private final TdAgentModelFactory modelFactory;

    /**
     * 构造器
     *
     * @param agentFactory             Agent 工厂
     * @param chatCommandService       命令服务
     * @param persistenceService       持久化服务
     * @param agentSessionStateService 会话状态服务
     * @param toolApprovalService      工具审批服务
     * @param providerRegistry         供应商注册表
     * @param modelFactory             模型工厂
     */
    public TdAgentChatServiceImpl(
            TdAgentFactory agentFactory,
            IChatCommandService chatCommandService,
            IConversationPersistenceService persistenceService,
            AgentSessionStateService agentSessionStateService,
            ToolApprovalService toolApprovalService,
            TdAgentProviderRegistry providerRegistry,
            TdAgentModelFactory modelFactory) {
        this.agentFactory = agentFactory;
        this.chatCommandService = chatCommandService;
        this.persistenceService = persistenceService;
        this.agentSessionStateService = agentSessionStateService;
        this.toolApprovalService = toolApprovalService;
        this.providerRegistry = providerRegistry;
        this.modelFactory = modelFactory;
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

    /**
     * 使用 LLM 为指定会话生成摘要标题。
     */
    @Override
    public String generateSessionTitle(String userId, String sessionId, String providerId, String modelId) {
        try {
            // 1. 加载会话历史，提取用户消息内容
            ConversationSessionContext context = ConversationSessionContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .build();
            ConversationMemoryDocument history =
                    persistenceService.getSessionHistory(userId, sessionId).orElse(null);
            if (history == null || history.getMessages() == null || history.getMessages().isEmpty()) {
                log.warn("会话历史为空，无法生成标题 - userId: {}, sessionId: {}", userId, sessionId);
                return null;
            }

            // 提取前几条用户消息的文本内容
            String userMessages = history.getMessages().stream()
                    .filter(msg -> "USER".equalsIgnoreCase(msg.getRole()))
                    .limit(3)
                    .flatMap(msg -> msg.getContent().stream())
                    .map(StoredMessageContent::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .reduce("", (a, b) -> a.isBlank() ? b : a + "\n" + b);

            if (userMessages.isBlank()) {
                log.warn("用户消息内容为空，无法生成标题 - userId: {}, sessionId: {}", userId, sessionId);
                return null;
            }

            // 2. 解析模型配置并创建模型实例
            TdAgentResolvedModelConfig resolvedConfig;
            try {
                resolvedConfig = providerRegistry.resolveForUser(userId, providerId, modelId);
            } catch (Exception e) {
                log.error("解析模型配置失败，无法生成标题 - userId: {}, error: {}", userId, e.getMessage());
                return null;
            }
            Model model = modelFactory.createFromConfig(resolvedConfig);

            // 3. 构造摘要 prompt 并调用 LLM
            String prompt = "请根据以下对话内容，生成一个简短的会话标题（不超过20个字符，不要包含引号或标点符号前缀）。\n"
                    + "只输出标题本身，不要输出任何其他内容。\n\n"
                    + "对话内容：\n" + userMessages;

            Msg titleMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(prompt)
                    .build();

            // 使用 Model.stream() 调用 LLM，收集完整响应
            List<io.agentscope.core.model.ChatResponse> responses = model.stream(List.of(titleMsg), null, null)
                    .collectList()
                    .block();

            String generatedTitle = null;
            if (responses != null && !responses.isEmpty()) {
                generatedTitle = responses.stream()
                        .flatMap(r -> r.getContent().stream())
                        .filter(TextBlock.class::isInstance)
                        .map(TextBlock.class::cast)
                        .map(TextBlock::getText)
                        .filter(text -> text != null && !text.isBlank())
                        .reduce("", (a, b) -> a + b)
                        .trim();
            }

            if (generatedTitle == null || generatedTitle.isBlank()) {
                log.warn("LLM 返回的标题为空 - userId: {}, sessionId: {}", userId, sessionId);
                return null;
            }

            // 4. 清理标题：去除引号、换行符，截断过长内容
            generatedTitle = generatedTitle.trim()
                    .replaceAll("[\"'`\n\r]", "")
                    .replaceAll("^标题[：:]\\s*", "");
            if (generatedTitle.length() > 30) {
                generatedTitle = generatedTitle.substring(0, 30);
            }

            // 5. 持久化标题
            persistenceService.updateSessionTitle(userId, sessionId, generatedTitle);
            log.info("LLM 标题生成成功 - userId: {}, sessionId: {}, title: {}", userId, sessionId, generatedTitle);

            return generatedTitle;
        } catch (Exception e) {
            log.error("生成会话标题失败 - userId: {}, sessionId: {}, error: {}", userId, sessionId, e.getMessage(), e);
            return null;
        }
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
