package com.liangshou.tangdynasty.agentic.agents.memory.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.common.exceptions.BizException;
import com.liangshou.tangdynasty.agentic.common.exceptions.ErrorCodeEnum;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.schemas.Session;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDBSessionHistoryService：基于 MongoDB 的 Session 历史服务实现。
 */
public class MongoDBSessionHistoryService implements SessionHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBSessionHistoryService.class);
    private static final ReplaceOptions UPSERT = new ReplaceOptions().upsert(true);

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final String collectionName;

    public MongoDBSessionHistoryService(MongoTemplate mongoTemplate) {
        this(mongoTemplate, "tang_agentic_session_history");
    }

    public MongoDBSessionHistoryService(MongoTemplate mongoTemplate, String collectionName) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.collectionName = collectionName;
    }

    private MongoCollection<Document> collection() {
        return mongoTemplate.getCollection(collectionName);
    }

    private String getDocumentId(String userId, String sessionId) {
        return "session:" + userId + ":" + sessionId;
    }

    private String sessionToJson(Session session) throws JsonProcessingException {
        return objectMapper.writeValueAsString(session);
    }

    private Session sessionFromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, Session.class);
    }

    @Override
    public CompletableFuture<Session> createSession(String userId, Optional<String> sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sid = sessionId.filter(s -> !s.trim().isEmpty())
                        .orElse(UUID.randomUUID().toString());

                Session session = new Session(sid, userId, new ArrayList<>());
                String documentId = getDocumentId(userId, sid);

                Document doc = new Document("_id", documentId)
                        .append("userId", userId)
                        .append("sessionId", sid)
                        .append("sessionJson", sessionToJson(session))
                        .append("createdAt", new Date())
                        .append("updatedAt", new Date());

                collection().replaceOne(Filters.eq("_id", documentId), doc, UPSERT);
                return session;
            } catch (Exception e) {
                LOGGER.error("Failed to create session in MongoDB. userId={}, sessionId={}", userId, sessionId.orElse(null));
                throw new BizException(ErrorCodeEnum.SESSION_HISTORY_ERROR, e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Session>> getSession(String userId, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String documentId = getDocumentId(userId, sessionId);
                Document doc = collection().find(Filters.eq("_id", documentId)).first();

                if (doc == null) {
                    Session session = new Session(sessionId, userId, new ArrayList<>());
                    Document newDoc = new Document("_id", documentId)
                            .append("userId", userId)
                            .append("sessionId", sessionId)
                            .append("sessionJson", sessionToJson(session))
                            .append("createdAt", new Date())
                            .append("updatedAt", new Date());
                    collection().replaceOne(Filters.eq("_id", documentId), newDoc, UPSERT);
                    return Optional.of(session);
                }

                String sessionJson = doc.getString("sessionJson");
                if (sessionJson == null || sessionJson.isBlank()) {
                    Session session = new Session(sessionId, userId, new ArrayList<>());
                    Date createdAt = doc.getDate("createdAt");
                    if (createdAt == null) {
                        createdAt = new Date();
                    }
                    Document repaired = new Document("_id", documentId)
                            .append("userId", userId)
                            .append("sessionId", sessionId)
                            .append("sessionJson", sessionToJson(session))
                            .append("createdAt", createdAt)
                            .append("updatedAt", new Date());
                    collection().replaceOne(Filters.eq("_id", documentId), repaired, UPSERT);
                    return Optional.of(session);
                }

                Session session = sessionFromJson(sessionJson);
                if (session.getMessages() == null) {
                    session.setMessages(new ArrayList<>());
                }
                return Optional.of(session);
            } catch (Exception e) {
                LOGGER.error("Failed to get session from MongoDB. userId={}, sessionId={}", userId, sessionId);
                throw new BizException(ErrorCodeEnum.SESSION_HISTORY_ERROR, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteSession(String userId, String sessionId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String documentId = getDocumentId(userId, sessionId);
                collection().deleteOne(Filters.eq("_id", documentId));
            } catch (Exception e) {
                LOGGER.error("Failed to delete session from MongoDB. userId={}, sessionId={}", userId, sessionId);
                throw new BizException(ErrorCodeEnum.SESSION_HISTORY_ERROR, e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Session>> listSessions(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Session> sessions = new ArrayList<>();
                for (Document doc : collection().find(Filters.eq("userId", userId))) {
                    String sessionJson = doc.getString("sessionJson");
                    if (sessionJson == null || sessionJson.isBlank()) {
                        continue;
                    }
                    Session session = sessionFromJson(sessionJson);
                    session.setMessages(new ArrayList<>());
                    sessions.add(session);
                }
                return sessions;
            } catch (Exception e) {
                LOGGER.error("Failed to list sessions from MongoDB. userId={}", userId);
                throw new BizException(ErrorCodeEnum.SESSION_HISTORY_ERROR, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> appendMessage(Session session, List<Message> messages) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (messages == null || messages.isEmpty()) {
                    return;
                }

                if (session.getMessages() == null) {
                    session.setMessages(new ArrayList<>());
                }
                session.getMessages().addAll(messages);

                String userId = session.getUserId();
                String sessionId = session.getId();
                String documentId = getDocumentId(userId, sessionId);

                Document doc = collection().find(Filters.eq("_id", documentId)).first();
                if (doc == null) {
                    LOGGER.warn("Session {} not found in MongoDB storage for append_message. userId={}", sessionId, userId);
                    return;
                }

                String sessionJson = doc.getString("sessionJson");
                Session storedSession = sessionJson == null || sessionJson.isBlank()
                        ? new Session(sessionId, userId, new ArrayList<>())
                        : sessionFromJson(sessionJson);

                if (storedSession.getMessages() == null) {
                    storedSession.setMessages(new ArrayList<>());
                }
                storedSession.getMessages().addAll(messages);

                Date createdAt = doc.getDate("createdAt");
                if (createdAt == null) {
                    createdAt = new Date();
                }
                Document updated = new Document("_id", documentId)
                        .append("userId", userId)
                        .append("sessionId", sessionId)
                        .append("sessionJson", sessionToJson(storedSession))
                        .append("createdAt", createdAt)
                        .append("updatedAt", new Date());
                collection().replaceOne(Filters.eq("_id", documentId), updated, UPSERT);
            } catch (Exception e) {
                LOGGER.error("Failed to append message to session in MongoDB. userId={}, sessionId={}", session.getUserId(), session.getId());
                throw new BizException(ErrorCodeEnum.SESSION_HISTORY_ERROR, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document result = mongoTemplate.executeCommand(new Document("ping", 1));
                Object ok = result.get("ok");
                if (ok instanceof Number number) {
                    return number.doubleValue() == 1.0d;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });
    }
}
