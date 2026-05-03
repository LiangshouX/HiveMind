package com.liangshou.tangdynasty.agentic.agents.tools;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.application.IConversationPersistenceService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * Agent 内置工具集 - 提供会话记忆搜索和概览查询功能。
 *
 * <p>该工具集为 Agent 提供以下能力：</p>
 * <ul>
 *     <li><b>search_session_memory</b>：在当前会话中搜索历史消息和工具执行记录，支持关键词检索和结果数量限制</li>
 *     <li><b>get_session_overview</b>：获取当前会话的概览信息，包括用户 ID、会话 ID、标题、消息总数和最近消息预览</li>
 * </ul>
 *
 * <p>这些工具使 Agent 能够：</p>
 * <ul>
 *     <li>回顾之前的对话内容，避免重复回答</li>
 *     <li>理解当前会话的上下文和进展</li>
 *     <li>向用户提供会话状态信息</li>
 * </ul>
 *
 * <p>注意：该类不是 Spring Component，而是在创建 Agent 时动态实例化，
 * 以便注入当前会话的 {@link ConversationSessionContext}。</p>
 *
 * @author LiangshouX
 */
public class TdAgentBuiltinTools {

    private final ConversationSessionContext context;
    private final IConversationPersistenceService persistenceService;

    /**
     * 构造器
     *
     * @param context            会话上下文
     * @param persistenceService 持久化服务
     */
    public TdAgentBuiltinTools(
            ConversationSessionContext context, IConversationPersistenceService persistenceService) {
        this.context = context;
        this.persistenceService = persistenceService;
    }

    /**
     * 搜索会话记忆。
     *
     * @param query 查询内容
     * @param limit 数量限制
     * @return 返回结果
     */
    @Tool(name = "search_session_memory", description = "搜索当前会话中的历史消息与工具执行记录")
    public String searchSessionMemory(
            @ToolParam(name = "query", description = "用于检索当前会话历史的关键词") String query,
            @ToolParam(name = "limit", description = "最多返回的匹配条数，推荐 1 到 10") Integer limit) {
        int safeLimit = limit == null ? 5 : Math.max(1, Math.min(limit, 10));
        return String.join(
                System.lineSeparator(),
                persistenceService.searchSessionMemory(context, query, safeLimit));
    }

    /**
     * 获取会话概览。
     *
     * @return 返回结果
     */
    @Tool(name = "get_session_overview", description = "获取当前会话的概览信息和最近的上下文摘要")
    public String getSessionOverview() {
        long messageCount = persistenceService.countMessages(context);
        String preview = persistenceService.buildRecentPreview(context, 6);
        return """
                userId=%s
                sessionId=%s
                title=%s
                messageCount=%d
                recentPreview=
                %s
                """
                .formatted(
                        context.getUserId(),
                        context.getSessionId(),
                        context.getSessionTitle() == null ? "" : context.getSessionTitle(),
                        messageCount,
                        preview == null || preview.isBlank() ? "暂无历史消息" : preview);
    }
}

