package com.liangshou.agentic.domain.session.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Agent 会话状态文档 - MongoDB 中存储 Agent 运行时状态的持久化对象。
 *
 * <p>该文档包含以下字段：</p>
 * <ul>
 *     <li>{@code id} - 主键，格式为 "{userId}:{sessionId}:state"</li>
 *     <li>{@code sessionId} - 会话唯一标识</li>
 *     <li>{@code userId} - 用户唯一标识</li>
 *     <li>{@code stateJson} - Agent 状态的 JSON 序列化字符串，包括记忆、工具历史等</li>
 *     <li>{@code paused} - 标记会话是否处于暂停状态</li>
 *     <li>{@code updatedAt} - 最后更新时间戳</li>
 * </ul>
 *
 * <p>索引策略：</p>
 * <ul>
 *     <li>复合唯一索引 (userId, session_id) 确保每个用户的每个会话只有一条状态记录</li>
 * </ul>
 *
 * <p>该文档由 {@link com.liangshou.agentic.agents.session.MongoAgentSession} 读写，
 * 支持 Agent 状态的跨请求持久化和恢复。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_session_state")
@CompoundIndex(name = "uk_state_user_session", def = "{'userId': 1, 'session_id': 1}", unique = true)
public class AgentSessionStateDocument {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    private String userId;

    @Field("state_json")
    private String stateJson;

    @Field("paused")
    private boolean paused;

    /**
     * 首轮对话使用的供应商 ID，用于审批/拒绝恢复时保持供应商一致性。
     */
    @Field("provider_id")
    private String providerId;

    /**
     * 首轮对话使用的模型 ID，用于审批/拒绝恢复时保持模型一致性。
     */
    @Field("model_id")
    private String modelId;

    private Instant updatedAt;
}
