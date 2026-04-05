package com.liangshou.tangdynasty.agentic.service;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.domain.document.ConversationMemoryDocument;
import com.liangshou.tangdynasty.agentic.domain.document.ConversationViewDocument;
import com.liangshou.tangdynasty.agentic.domain.document.StoredMessage;
import com.liangshou.tangdynasty.agentic.domain.document.StoredMessageContent;
import com.liangshou.tangdynasty.agentic.repository.ConversationMemoryRepository;
import com.liangshou.tangdynasty.agentic.repository.ConversationViewRepository;
import io.agentscope.core.message.Msg;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LiangshouX
 */
@Service
public class ConversationPersistenceService {

    private final ConversationMemoryRepository conversationMemoryRepository;
    private final ConversationViewRepository conversationViewRepository;
    private final MessageMapper messageMapper;

    /**
     * 执行相关操作。
     *
     * @param conversationMemoryRepository 历史 Repository
     * @param conversationViewRepository   会话视图 Repository
     * @param messageMapper                消息映射器
     */
    public ConversationPersistenceService(
            ConversationMemoryRepository conversationMemoryRepository,
            ConversationViewRepository conversationViewRepository,
            MessageMapper messageMapper) {
        this.conversationMemoryRepository = conversationMemoryRepository;
        this.conversationViewRepository = conversationViewRepository;
        this.messageMapper = messageMapper;
    }

    /**
     * 加载历史消息。
     *
     * @param context 会话上下文
     * @return 返回结果
     */
    public List<Msg> loadMessages(ConversationSessionContext context) {
        return loadMemoryDocument(context)
                .map(ConversationMemoryDocument::getMessages)
                .orElse(List.of())
                .stream()
                .map(messageMapper::toMsg)
                .toList();
    }

    /**
     * 替换消息列表。
     *
     * @param context      会话上下文
     * @param messages     消息列表
     * @param systemPrompt system prompt
     * @return 返回结果
     */
    public ConversationMemoryDocument replaceMessages(
            ConversationSessionContext context,
            List<Msg> messages,
            String systemPrompt,
            String compressedSummary,
            boolean compactionUpdated) {
        Instant now = Instant.now();
        List<StoredMessage> storedMessages =
                messages.stream().map(messageMapper::toStoredMessage).toList();
        ConversationMemoryDocument document =
                loadMemoryDocument(context)
                        .orElseGet(() -> newMemoryDocument(context, now, systemPrompt));
        document.setMessages(new ArrayList<>(storedMessages));
        document.setRoundCount(calculateRoundCount(messages));
        document.setUpdatedAt(now);
        document.setSysPrompt(systemPrompt);
        document.setCompressedSummary(compressedSummary);
        if (compactionUpdated) {
            document.setCompactionCount(
                    document.getCompactionCount() == null ? 1L : document.getCompactionCount() + 1);
            document.setSummaryUpdatedAt(now);
        }
        document.setTitle(resolveTitle(context, storedMessages));
        ConversationMemoryDocument saved = conversationMemoryRepository.save(document);
        saveOrUpdateView(context, saved.getTitle(), storedMessages.size(), now);
        return saved;
    }

    /**
     * 执行 clearSession 操作。
     *
     * @param context 会话上下文
     */
    public void clearSession(ConversationSessionContext context) {
        loadMemoryDocument(context).ifPresent(document -> {
            document.setMessages(new ArrayList<>());
            document.setRoundCount(0L);
            document.setCompressedSummary("");
            document.setUpdatedAt(Instant.now());
            conversationMemoryRepository.save(document);
        });
        saveOrUpdateView(context, defaultTitle(context), 0, Instant.now());
    }

    /**
     * 加载压缩摘要。
     *
     * @param context 会话上下文
     * @return 返回结果
     */
    public String loadCompressedSummary(ConversationSessionContext context) {
        return loadMemoryDocument(context)
                .map(ConversationMemoryDocument::getCompressedSummary)
                .orElse("");
    }

    /**
     * 列出会话列表。
     *
     * @param userId 用户标识
     * @return 返回结果
     */
    public List<ConversationViewDocument> listSessions(String userId) {
        return conversationViewRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 执行 getSessionView 操作。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    public Optional<ConversationViewDocument> getSessionView(String userId, String sessionId) {
        return conversationViewRepository.findByUserIdAndSessionId(userId, sessionId);
    }

    /**
     * 获取会话历史。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    public Optional<ConversationMemoryDocument> getSessionHistory(String userId, String sessionId) {
        return conversationMemoryRepository.findByUserIdAndSessionId(userId, sessionId);
    }

    /**
     * 搜索会话记忆。
     *
     * @param context 会话上下文
     * @param query   查询内容
     * @param limit   数量限制
     * @return 返回结果
     */
    public List<String> searchSessionMemory(
            ConversationSessionContext context, String query, int limit) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return loadMemoryDocument(context)
                .map(ConversationMemoryDocument::getMessages)
                .orElse(List.of())
                .stream()
                .filter(message -> containsQuery(message, normalized))
                .sorted(Comparator.comparing(StoredMessage::getTimestamp).reversed())
                .limit(Math.max(1, limit))
                .map(this::formatMessage)
                .collect(Collectors.toList());
    }

    /**
     * 统计消息数量。
     *
     * @param context 会话上下文
     * @return 返回结果
     */
    public long countMessages(ConversationSessionContext context) {
        return loadMemoryDocument(context)
                .map(ConversationMemoryDocument::getMessages)
                .map(List::size)
                .orElse(0);
    }

    /**
     * 执行 buildRecentPreview 操作。
     *
     * @param context 会话上下文
     * @param limit   数量限制
     * @return 返回结果
     */
    public String buildRecentPreview(ConversationSessionContext context, int limit) {
        return loadMemoryDocument(context)
                .map(ConversationMemoryDocument::getMessages)
                .orElse(List.of())
                .stream()
                .skip(Math.max(0, countMessages(context) - Math.max(limit, 1)))
                .map(this::formatMessage)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private Optional<ConversationMemoryDocument> loadMemoryDocument(ConversationSessionContext context) {
        return conversationMemoryRepository.findByUserIdAndSessionId(context.getUserId(), context.getSessionId());
    }

    private ConversationMemoryDocument newMemoryDocument(
            ConversationSessionContext context, Instant now, String systemPrompt) {
        return ConversationMemoryDocument.builder()
                .id(context.documentId())
                .sessionId(context.getSessionId())
                .userId(context.getUserId())
                .messages(new ArrayList<>())
                .roundCount(0L)
                .createdAt(now)
                .updatedAt(now)
                .title(defaultTitle(context))
                .sysPrompt(systemPrompt)
                .compressedSummary("")
                .compactionCount(0L)
                .build();
    }

    private void saveOrUpdateView(
            ConversationSessionContext context, String title, long messageCount, Instant now) {
        ConversationViewDocument view =
                conversationViewRepository
                        .findByUserIdAndSessionId(context.getUserId(), context.getSessionId())
                        .orElseGet(() -> ConversationViewDocument.builder()
                                .id(context.documentId())
                                .sessionId(context.getSessionId())
                                .userId(context.getUserId())
                                .createdAt(now)
                                .build());
        view.setTitle(title);
        view.setMessageCount(messageCount);
        view.setUnreadCount(0);
        view.setLastMessageAt(messageCount == 0 ? null : now);
        view.setUpdatedAt(now);
        conversationViewRepository.save(view);
    }

    private boolean containsQuery(StoredMessage message, String normalized) {
        if (message.getRole() != null && message.getRole().toLowerCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        if (message.getName() != null && message.getName().toLowerCase(Locale.ROOT).contains(normalized)) {
            return true;
        }
        return message.getContent().stream().anyMatch(content -> matchesContent(content, normalized));
    }

    private boolean matchesContent(StoredMessageContent content, String normalized) {
        return contains(content.getText(), normalized)
                || contains(content.getName(), normalized)
                || contains(content.getInput(), normalized)
                || contains(content.getInputRaw(), normalized);
    }

    private boolean contains(String raw, String normalized) {
        return raw != null && raw.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private String formatMessage(StoredMessage message) {
        String text = message.getContent().stream()
                .map(content -> firstNonBlank(content.getText(), content.getInputRaw(), content.getInput()))
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" | "));
        return "[" + firstNonBlank(message.getTimestamp(), "-") + "] "
                + firstNonBlank(message.getRole(), "UNKNOWN")
                + ": "
                + text;
    }

    private String defaultTitle(ConversationSessionContext context) {
        return firstNonBlank(context.getSessionTitle(), "Session-" + context.getSessionId());
    }

    private String resolveTitle(ConversationSessionContext context, List<StoredMessage> messages) {
        if (context.getSessionTitle() != null && !context.getSessionTitle().isBlank()) {
            return context.getSessionTitle();
        }
        return messages.stream()
                .flatMap(message -> message.getContent().stream())
                .map(StoredMessageContent::getText)
                .filter(text -> text != null && !text.isBlank())
                .map(text -> text.length() > 32 ? text.substring(0, 32) : text)
                .findFirst()
                .orElse(defaultTitle(context));
    }

    private long calculateRoundCount(List<Msg> messages) {
        return messages.stream()
                .filter(message -> message.getRole() != null && "USER".equals(message.getRole().name()))
                .count();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}

