package com.liangshou.tangdynasty.agentic.agents.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.domain.document.AgentSessionStateDocument;
import com.liangshou.tangdynasty.agentic.repository.AgentSessionStateRepository;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.State;

import java.time.Instant;
import java.util.*;

/**
 * @author LiangshouX
 */
public class MongoAgentSession implements Session {

    private final AgentSessionStateRepository repository;
    private final ObjectMapper objectMapper;
    private final String userId;

    /**
     * 执行相关操作。
     *
     * @param repository   参数
     * @param objectMapper ObjectMapper
     * @param userId       用户标识
     */
    public MongoAgentSession(
            AgentSessionStateRepository repository, ObjectMapper objectMapper, String userId) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.userId = userId;
    }

    @Override
    public void save(SessionKey sessionKey, String key, State value) {
        Map<String, Object> payload = loadPayload(sessionKey);
        payload.put(key, value);
        persist(sessionKey, payload);
    }

    @Override
    public void save(SessionKey sessionKey, String key, List<? extends State> values) {
        Map<String, Object> payload = loadPayload(sessionKey);
        payload.put(key, values);
        persist(sessionKey, payload);
    }

    @Override
    public <T extends State> Optional<T> get(SessionKey sessionKey, String key, Class<T> type) {
        Object value = loadPayload(sessionKey).get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(value, type));
    }

    @Override
    public <T extends State> List<T> getList(SessionKey sessionKey, String key, Class<T> itemType) {
        Object value = loadPayload(sessionKey).get(key);
        if (value == null) {
            return List.of();
        }
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return objectMapper.convertValue(value, listType);
    }

    @Override
    public boolean exists(SessionKey sessionKey) {
        return repository.findByUserIdAndSessionId(userId, sessionKey.toString()).isPresent();
    }

    @Override
    public void delete(SessionKey sessionKey) {
        repository.findByUserIdAndSessionId(userId, sessionKey.toString()).ifPresent(repository::delete);
    }

    @Override
    public Set<SessionKey> listSessionKeys() {
        return Set.of();
    }

    private void persist(SessionKey sessionKey, Map<String, Object> payload) {
        AgentSessionStateDocument document =
                repository.findByUserIdAndSessionId(userId, sessionKey.toString())
                        .orElseGet(
                                () ->
                                        AgentSessionStateDocument.builder()
                                                .id(userId + ":" + sessionKey + ":state")
                                                .userId(userId)
                                                .sessionId(sessionKey.toString())
                                                .build());
        document.setStateJson(write(payload));
        document.setUpdatedAt(Instant.now());
        repository.save(document);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadPayload(SessionKey sessionKey) {
        return repository.findByUserIdAndSessionId(userId, sessionKey.toString())
                .map(AgentSessionStateDocument::getStateJson)
                .filter(json -> json != null && !json.isBlank())
                .map(this::read)
                .orElseGet(LinkedHashMap::new);
    }

    private Map<String, Object> read(String json) {
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize session state.", ex);
        }
    }

    private String write(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize session state.", ex);
        }
    }
}

