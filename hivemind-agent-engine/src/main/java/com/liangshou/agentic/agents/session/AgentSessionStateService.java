package com.liangshou.agentic.agents.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.domain.session.model.AgentSessionStateDocument;
import com.liangshou.agentic.infrastructure.mongo.repository.AgentSessionStateRepository;
import io.agentscope.core.ReActAgent;
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
 * <p>在 AgentScope 2.0 中，状态管理通过 {@link io.agentscope.core.state.AgentStateStore} 实现。
 * {@link ReActAgent} 在 builder 中配置 {@code stateStore} 后，会自动在每次调用时
 * 加载和保存状态，无需手动调用 {@code saveTo}/{@code loadFrom}。</p>
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
     * 创建 MongoDB AgentStateStore 实例。
     *
     * <p>该方法供 {@link com.liangshou.agentic.agents.TdAgentFactory} 调用，
     * 将创建的 store 传给 {@link ReActAgent.Builder#stateStore}。</p>
     *
     * @return MongoDB AgentStateStore 实例
     */
    public MongoAgentStateStore createStateStore() {
        return new MongoAgentStateStore(agentSessionStateRepository, objectMapper);
    }

    /**
     * 保存暂停状态标记。
     *
     * <p>在 AgentScope 2.0 中，Agent 状态由 AgentStateStore 自动管理。
     * 此方法仅更新暂停标记和模型配置元数据。</p>
     *
     * @param context 会话上下文
     * @param agent   Agent 实例（保留参数以保持接口兼容）
     * @param paused  是否处于暂停状态
     */
    public void save(ConversationSessionContext context, ReActAgent agent, boolean paused) {
        agentSessionStateRepository
                .findByUserIdAndSessionId(effectiveUserId(context), context.getSessionId())
                .ifPresent(
                        document -> {
                            document.setPaused(paused);
                            document.setProviderId(context.getProviderId());
                            document.setModelId(context.getModelId());
                            document.setUpdatedAt(Instant.now());
                            agentSessionStateRepository.save(document);
                        });
    }

    /**
     * 执行 isPaused 操作。
     * @param context 会话上下文
     * @return 返回结果
     */
    public boolean isPaused(ConversationSessionContext context) {
        return agentSessionStateRepository
                .findByUserIdAndSessionId(effectiveUserId(context), context.getSessionId())
                .map(AgentSessionStateDocument::isPaused)
                .orElse(false);
    }

    /**
     * 获取会话状态中保存的供应商 ID。
     * @param context 会话上下文
     * @return 供应商 ID，无记录时返回 null
     */
    public String getStoredProviderId(ConversationSessionContext context) {
        return agentSessionStateRepository
                .findByUserIdAndSessionId(effectiveUserId(context), context.getSessionId())
                .map(AgentSessionStateDocument::getProviderId)
                .orElse(null);
    }

    /**
     * 获取会话状态中保存的模型 ID。
     * @param context 会话上下文
     * @return 模型 ID，无记录时返回 null
     */
    public String getStoredModelId(ConversationSessionContext context) {
        return agentSessionStateRepository
                .findByUserIdAndSessionId(effectiveUserId(context), context.getSessionId())
                .map(AgentSessionStateDocument::getModelId)
                .orElse(null);
    }

    /**
     * 清理当前数据。
     * @param context 会话上下文
     */
    public void clear(ConversationSessionContext context) {
        agentSessionStateRepository
                .findByUserIdAndSessionId(effectiveUserId(context), context.getSessionId())
                .ifPresent(agentSessionStateRepository::delete);
    }

    /**
     * 归一化 userId：与 {@link MongoAgentStateStore} 的 {@code null → ""} 映射保持一致，
     * 避免匿名会话（userId=null）查询不到存储侧写入的文档。
     */
    private String effectiveUserId(ConversationSessionContext context) {
        return context.getUserId() == null ? "" : context.getUserId();
    }
}
