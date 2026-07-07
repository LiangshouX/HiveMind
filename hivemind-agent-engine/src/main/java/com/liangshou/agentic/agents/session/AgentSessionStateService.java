package com.liangshou.agentic.agents.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.domain.session.model.AgentSessionStateDocument;
import com.liangshou.agentic.infrastructure.mongo.repository.AgentSessionStateRepository;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.SimpleSessionKey;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Agent 会话状态服务 - 管理 ReActAgent 的状态持久化和恢复。
 *
 * <p>该服务提供以下核心功能：</p>
 * <ul>
 *     <li><b>状态保存</b>：将 Agent 的当前状态（包括记忆、工具历史等）序列化并保存到 MongoDB</li>
 *     <li><b>状态恢复</b>：从存储中加载之前保存的 Agent 状态，实现对话断点续传</li>
 *     <li><b>暂停标记</b>：记录会话是否处于暂停状态，用于控制 Agent 的执行流程</li>
 *     <li><b>状态清理</b>：删除指定会话的状态数据，用于重置会话</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>用户刷新页面后恢复之前的对话上下文</li>
 *     <li>长时间运行的任务需要暂停和恢复</li>
 *     <li>多设备间同步 Agent 状态</li>
 * </ul>
 *
 * <p>底层通过 {@link MongoAgentSession} 实现 AgentScope 的 {@link io.agentscope.core.session.Session} 接口，
 * 将 Agent 状态以 JSON 格式存储在 {@link AgentSessionStateDocument} 中。</p>
 *
 * @author LiangshouX
 */
@Service
public class AgentSessionStateService {

    private final AgentSessionStateRepository agentSessionStateRepository;
    private final ObjectMapper objectMapper;

    /**
     * 构造器
     * @param agentSessionStateRepository 状态 Repository
     * @param objectMapper ObjectMapper
     */
    public AgentSessionStateService(
            AgentSessionStateRepository agentSessionStateRepository, ObjectMapper objectMapper) {
        this.agentSessionStateRepository = agentSessionStateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存当前状态。
     * @param context 会话上下文
     * @param agent 参数
     * @param paused 是否处于暂停状态
     */
    public void save(ConversationSessionContext context, ReActAgent agent, boolean paused) {
        MongoAgentSession session =
                new MongoAgentSession(agentSessionStateRepository, objectMapper, context.getUserId());
        agent.saveTo(session, SimpleSessionKey.of(context.getSessionId()));
        agentSessionStateRepository
                .findByUserIdAndSessionId(context.getUserId(), context.getSessionId())
                .ifPresent(
                        document -> {
                            document.setPaused(paused);
                            document.setUpdatedAt(Instant.now());
                            agentSessionStateRepository.save(document);
                        });
    }

    /**
     * 恢复已保存的状态。
     * @param context 会话上下文
     * @param agent 参数
     */
    public void restore(ConversationSessionContext context, ReActAgent agent) {
        if (agentSessionStateRepository
                .findByUserIdAndSessionId(context.getUserId(), context.getSessionId())
                .isEmpty()) {
            return;
        }
        MongoAgentSession session =
                new MongoAgentSession(agentSessionStateRepository, objectMapper, context.getUserId());
        agent.loadFrom(session, SimpleSessionKey.of(context.getSessionId()));
    }

    /**
     * 执行 isPaused 操作。
     * @param context 会话上下文
     * @return 返回结果
     */
    public boolean isPaused(ConversationSessionContext context) {
        return agentSessionStateRepository
                .findByUserIdAndSessionId(context.getUserId(), context.getSessionId())
                .map(AgentSessionStateDocument::isPaused)
                .orElse(false);
    }

    /**
     * 清理当前数据。
     * @param context 会话上下文
     */
    public void clear(ConversationSessionContext context) {
        agentSessionStateRepository
                .findByUserIdAndSessionId(context.getUserId(), context.getSessionId())
                .ifPresent(agentSessionStateRepository::delete);
    }
}

