package com.liangshou.agentic.agents.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import com.liangshou.agentic.domain.session.model.AgentSessionStateDocument;
import com.liangshou.agentic.infrastructure.mongo.repository.AgentSessionStateRepository;
import io.agentscope.core.message.Msg;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB Agent 状态存储 - 实现 AgentScope 2.0 的 AgentStateStore 接口。
 *
 * <p>该类将 AgentScope 2.0 的 {@link AgentStateStore} 抽象适配到 MongoDB 存储，
 * 替代旧版的 {@link MongoAgentSession}（基于 Session/SessionKey 的旧 API）。</p>
 *
 * <p>与旧版的关键区别：</p>
 * <ul>
 *     <li>使用 {@code (userId, sessionId, key)} 三元组寻址，而非 {@code (SessionKey, key)}</li>
 *     <li>直接实现 {@link AgentStateStore} 接口，不再依赖已移除的 {@code Session} 接口</li>
 *     <li>支持 {@link #listSessionIds(String)} 查询用户的所有会话</li>
 * </ul>
 *
 * @author LiangshouX
 * @see AgentStateStore
 */
public class MongoAgentStateStore implements AgentStateStore {

    private static final Logger log = LoggerFactory.getLogger(MongoAgentStateStore.class);

    private final AgentSessionStateRepository repository;
    private final ObjectMapper objectMapper;

    public MongoAgentStateStore(
            AgentSessionStateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        // 配置 ObjectMapper 忽略未知属性，兼容 1.x 版本存储的旧格式数据
        // 例如旧版 ChatUsage 中的 "totalTokens" 字段在 2.0.0 中已变为计算属性
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void save(String userId, String sessionId, String key, State value) {
        Map<String, Object> payload = loadPayload(userId, sessionId);
        payload.put(key, value);
        persist(userId, sessionId, payload);
    }

    @Override
    public void save(String userId, String sessionId, String key, List<? extends State> values) {
        Map<String, Object> payload = loadPayload(userId, sessionId);
        payload.put(key, values);
        persist(userId, sessionId, payload);
    }

    @Override
    public <T extends State> Optional<T> get(String userId, String sessionId, String key, Class<T> type) {
        Object value = loadPayload(userId, sessionId).get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(value, type));
    }

    @Override
    public <T extends State> List<T> getList(String userId, String sessionId, String key, Class<T> itemType) {
        Object value = loadPayload(userId, sessionId).get(key);
        if (value == null) {
            return List.of();
        }
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return objectMapper.convertValue(value, listType);
    }

    @Override
    public boolean exists(String userId, String sessionId) {
        String docId = buildDocId(userId, sessionId);
        return repository.findById(docId).isPresent();
    }

    @Override
    public void delete(String userId, String sessionId) {
        String docId = buildDocId(userId, sessionId);
        repository.findById(docId).ifPresent(repository::delete);
    }

    @Override
    public Set<String> listSessionIds(String userId) {
        return repository.findByUserId(userId == null ? "" : userId)
                .stream()
                .map(AgentSessionStateDocument::getSessionId)
                .collect(Collectors.toSet());
    }

    /**
     * 从 MongoDB 对话历史中预填充 AgentState。
     *
     * <p>在 AgentScope 2.0 中，Agent 的对话上下文由 AgentState 管理。
     * 此方法从 {@link IConversationPersistenceService} 加载历史消息，
     * 并将其写入 AgentStateStore，使 Agent 在首次调用时能看到之前的对话历史。</p>
     *
     * <p>如果 AgentStateStore 中已有该会话的状态（即非首次调用），则跳过预填充。</p>
     *
     * @param context             会话上下文
     * @param persistenceService  对话持久化服务
     */
    public void prePopulateFromHistory(
            ConversationSessionContext context,
            IConversationPersistenceService persistenceService) {
        String userId = context.getUserId();
        String sessionId = context.getSessionId();

        // 如果已有状态，跳过预填充
        if (exists(userId, sessionId)) {
            log.debug("[MongoAgentStateStore] 会话状态已存在，跳过预填充 - userId: {}, sessionId: {}",
                    userId, sessionId);
            return;
        }

        // 从 MongoDB 加载历史消息
        List<Msg> historyMessages = persistenceService.loadMessages(context);
        if (historyMessages.isEmpty()) {
            log.debug("[MongoAgentStateStore] 无历史消息，跳过预填充 - userId: {}, sessionId: {}",
                    userId, sessionId);
            return;
        }

        // 创建 AgentState 并填充历史消息
        AgentState agentState = AgentState.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        agentState.contextMutable().addAll(historyMessages);

        // 保存到 AgentStateStore
        save(userId, sessionId, "agent_state", agentState);
        log.info("[MongoAgentStateStore] 已预填充对话历史 - userId: {}, sessionId: {}, messages: {}",
                userId, sessionId, historyMessages.size());
    }

    private void persist(String userId, String sessionId, Map<String, Object> payload) {
        String docId = buildDocId(userId, sessionId);
        String effectiveUserId = userId == null ? "" : userId;
        AgentSessionStateDocument document =
                repository.findById(docId)
                        .orElseGet(() -> AgentSessionStateDocument.builder()
                                .id(docId)
                                .userId(effectiveUserId)
                                .sessionId(sessionId)
                                .build());
        document.setStateJson(write(payload));
        document.setUpdatedAt(Instant.now());
        repository.save(document);
    }

    private Map<String, Object> loadPayload(String userId, String sessionId) {
        String docId = buildDocId(userId, sessionId);
        return repository.findById(docId)
                .map(AgentSessionStateDocument::getStateJson)
                .filter(json -> json != null && !json.isBlank())
                .map(this::read)
                .orElseGet(LinkedHashMap::new);
    }

    private String buildDocId(String userId, String sessionId) {
        String effectiveUserId = userId == null ? "" : userId;
        return effectiveUserId + ":" + sessionId + ":state";
    }

    private Map<String, Object> read(String json) {
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException ex) {
            throw new BizException(HmeErrorCode.AGENT_SESSION_DESERIALIZE_ERROR, ex);
        }
    }

    private String write(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BizException(HmeErrorCode.AGENT_SESSION_SERIALIZE_ERROR, ex);
        }
    }
}
