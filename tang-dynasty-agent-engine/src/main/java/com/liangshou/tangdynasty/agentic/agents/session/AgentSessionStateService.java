package com.liangshou.tangdynasty.agentic.agents.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.repository.AgentSessionStateRepository;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.state.SimpleSessionKey;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @author LiangshouX
 */
@Service
public class AgentSessionStateService {

    private final AgentSessionStateRepository agentSessionStateRepository;
    private final ObjectMapper objectMapper;

    /**
     * 执行相关操作。
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
        if (!agentSessionStateRepository
                .findByUserIdAndSessionId(context.getUserId(), context.getSessionId())
                .isPresent()) {
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
                .map(document -> document.isPaused())
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

