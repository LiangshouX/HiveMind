package com.liangshou.agentic.application.impl;

import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.application.IChatCommandService;
import com.liangshou.agentic.application.dto.ChatResponse;
import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.agentic.agents.memory.compaction.CompactionResult;
import com.liangshou.agentic.agents.session.AgentSessionStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 聊天命令处理服务，负责解析和执行用户输入的特殊命令。
 * <p>
 * 支持的命令包括：
 * <ul>
 *   <li>/clear - 清空当前会话的历史消息和状态</li>
 *   <li>/history - 显示最近 20 条会话历史预览</li>
 *   <li>/new - 生成新的会话 ID，用于创建新会话</li>
 *   <li>/compress - 手动压缩当前会话的历史消息</li>
 * </ul>
 * </p>
 * <p>
 * 该服务在聊天流程中优先执行，如果检测到命令格式（以 / 开头），
 * 则直接处理并返回响应，不再调用 AI Agent。
 * </p>
 *
 * @author LiangshouX
 */
@Service
@SuppressWarnings("unused")
public class ChatCommandServiceImpl implements IChatCommandService {

    private static final Logger log = LoggerFactory.getLogger(ChatCommandServiceImpl.class);

    private final IConversationPersistenceService persistenceService;
    private final AgentSessionStateService agentSessionStateService;
    private final TdAgentMemoryManager memoryManager;

    /**
     * 构造器
     * @param persistenceService 持久化服务
     * @param agentSessionStateService 会话状态服务
     * @param memoryManager 记忆管理器
     */
    public ChatCommandServiceImpl(
            IConversationPersistenceService persistenceService,
            AgentSessionStateService agentSessionStateService,
            TdAgentMemoryManager memoryManager) {
        this.persistenceService = persistenceService;
        this.agentSessionStateService = agentSessionStateService;
        this.memoryManager = memoryManager;
    }

    /**
     * 处理命令。
     * @param context 会话上下文
     * @param rawMessage 原始用户消息
     * @return 返回结果
     */
    @Override
    public ChatResponse handleCommand(ConversationSessionContext context, String rawMessage) {
        if (rawMessage == null || !rawMessage.startsWith("/")) {
            return null;
        }
        String message = rawMessage.trim();
        if ("/clear".equalsIgnoreCase(message)) {
            persistenceService.clearSession(context);
            agentSessionStateService.clear(context);
            return buildResponse(
                    context,
                    "当前会话历史已清空。",
                    0,
                    Map.of("command", "clear"));
        }
        if ("/history".equalsIgnoreCase(message)) {
            String preview = persistenceService.buildRecentPreview(context, 20);
            return buildResponse(
                    context,
                    preview == null || preview.isBlank() ? "当前会话暂无历史消息。" : preview,
                    persistenceService.countMessages(context),
                    Map.of("command", "history"));
        }
        if ("/new".equalsIgnoreCase(message)) {
            String newSessionId = UUID.randomUUID().toString();
            return buildResponse(
                    context,
                    "已生成新的会话 ID，请在下一次请求中切换到该 sessionId。",
                    0,
                    Map.of("command", "new", "newSessionId", newSessionId));
        }
        if ("/compress".equalsIgnoreCase(message)) {
            return handleCompress(context);
        }
        return buildResponse(
                context,
                "暂不支持该命令。当前支持的命令：/new、/clear、/history、/compress",
                persistenceService.countMessages(context),
                Map.of("command", "unknown"));
    }

    /**
     * 处理 /compress 命令，手动压缩当前会话的历史消息。
     *
     * @param context 会话上下文
     * @return 压缩结果响应
     */
    private ChatResponse handleCompress(ConversationSessionContext context) {
        try {
            CompactionResult result = memoryManager.forceCompact(context, persistenceService);
            if (result == null) {
                return buildResponse(
                        context,
                        "当前会话消息数过少，无需压缩。",
                        persistenceService.countMessages(context),
                        Map.of("command", "compress", "compacted", false));
            }
            String message = String.format(
                    "压缩完成。消息: %d → %d（压缩 %d 条），tokens: %d → %d（减少 %.1f%%），策略: %s，耗时: %dms",
                    result.getMessagesBefore(),
                    result.getMessagesAfter(),
                    result.getMiddleCompressed(),
                    result.getTokensBefore(),
                    result.getTokensAfter(),
                    result.compressionRatio() * 100,
                    result.getStrategy(),
                    result.getDurationMs());
            return buildResponse(
                    context,
                    message,
                    persistenceService.countMessages(context),
                    Map.of("command", "compress", "compacted", true,
                            "messagesBefore", result.getMessagesBefore(),
                            "messagesAfter", result.getMessagesAfter(),
                            "tokensBefore", result.getTokensBefore(),
                            "tokensAfter", result.getTokensAfter(),
                            "strategy", result.getStrategy()));
        } catch (Exception e) {
            log.error("[ChatCommandService] 手动压缩失败 - sessionId: {}", context.getSessionId(), e);
            return buildResponse(
                    context,
                    "压缩失败: " + e.getMessage(),
                    persistenceService.countMessages(context),
                    Map.of("command", "compress", "compacted", false, "error", e.getMessage()));
        }
    }

}
