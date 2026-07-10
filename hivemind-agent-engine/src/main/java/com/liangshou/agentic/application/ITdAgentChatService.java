package com.liangshou.agentic.application;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.domain.memory.model.ConversationViewDocument;
import com.liangshou.agentic.application.dto.ChatRequest;
import com.liangshou.agentic.application.dto.ChatResponse;
import com.liangshou.agentic.application.dto.SessionHistoryResponse;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;

import java.util.List;

/**
 * Agent 聊天服务核心组件，负责处理用户与 AI Agent 之间的对话交互。
 *
 * @author LiangshouX
 */
public interface ITdAgentChatService {

    /**
     * 处理聊天请求。
     *
     * @param request 请求对象{@link ChatRequest}
     * @return 返回结果
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 列出会话列表。
     *
     * @param userId 用户标识
     * @return 返回结果
     */
    List<ConversationViewDocument> listSessions(String userId);

    /**
     * 获取会话历史。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    SessionHistoryResponse getSessionHistory(String userId, String sessionId);

    /**
     * 删除会话（包括会话视图和历史记录）。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     */
    void deleteSession(String userId, String sessionId);

    /**
     * 构建会话上下文。
     *
     * @param request 请求对象{@link ChatRequest}
     * @return {@link ConversationSessionContext}
     */
    ConversationSessionContext buildContext(ChatRequest request);

    /**
     * 构建会话上下文。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @param title     会话标题
     * @return 返回结果
     */
    ConversationSessionContext buildContext(String userId, String sessionId, String title);

    /**
     * 使用 LLM 为指定会话生成摘要标题。
     *
     * <p>该方法加载会话的前几条用户消息，通过 LLM 生成一个简短的标题摘要，
     * 并更新到 MongoDB 中的 ConversationViewDocument 和 ConversationMemoryDocument。</p>
     *
     * @param userId     用户标识
     * @param sessionId  会话标识
     * @param providerId 供应商 ID（可选，为 null 时使用默认供应商）
     * @param modelId    模型 ID（可选，为 null 时使用默认模型）
     * @return LLM 生成的标题；如果会话不存在或生成失败则返回 null
     */
    String generateSessionTitle(String userId, String sessionId, String providerId, String modelId);

    default boolean isPaused(Msg response) {
        if (response == null || response.getGenerateReason() == null) {
            return false;
        }
        return response.getGenerateReason() == GenerateReason.REASONING_STOP_REQUESTED
                || response.getGenerateReason() == GenerateReason.ACTING_STOP_REQUESTED
                || response.getGenerateReason() == GenerateReason.TOOL_SUSPENDED;
    }
}
