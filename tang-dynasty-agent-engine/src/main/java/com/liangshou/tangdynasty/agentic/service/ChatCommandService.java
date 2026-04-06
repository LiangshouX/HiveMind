package com.liangshou.tangdynasty.agentic.service;

import com.liangshou.tangdynasty.agentic.adapter.controller.dto.ChatResponse;
import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.session.AgentSessionStateService;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
public class ChatCommandService {

    private final ConversationPersistenceService persistenceService;
    private final AgentSessionStateService agentSessionStateService;

    /**
     * 执行相关操作。
     * @param persistenceService 持久化服务
     * @param agentSessionStateService 会话状态服务
     */
    public ChatCommandService(
            ConversationPersistenceService persistenceService,
            AgentSessionStateService agentSessionStateService) {
        this.persistenceService = persistenceService;
        this.agentSessionStateService = agentSessionStateService;
    }

    /**
     * 处理命令。
     * @param context 会话上下文
     * @param rawMessage 原始用户消息
     * @return 返回结果
     */
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
        return buildResponse(
                context,
                "暂不支持该命令。当前支持的命令：/new、/clear、/history",
                persistenceService.countMessages(context),
                Map.of("command", "unknown"));
    }

    private ChatResponse buildResponse(
            ConversationSessionContext context,
            String message,
            long messageCount,
            Map<String, Object> metadata) {
        return ChatResponse.builder()
                .success(true)
                .commandHandled(true)
                .userId(context.getUserId())
                .sessionId(context.getSessionId())
                .title(context.getSessionTitle())
                .message(message)
                .messageCount(messageCount)
                .timestamp(Instant.now().toString())
                .metadata(metadata)
                .build();
    }
}
