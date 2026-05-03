package com.liangshou.tangdynasty.agentic.domain.memory.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话记忆文档 - MongoDB 中存储完整对话历史的持久化对象。
 *
 * <p>该文档包含以下核心字段：</p>
 * <ul>
 *     <li>{@code id} - 主键，格式为 "{userId}:{sessionId}"</li>
 *     <li>{@code sessionId} / {@code userId} - 会话和用户的唯一标识</li>
 *     <li>{@code messages} - 完整的消息列表，每个消息包含角色、内容、时间戳等</li>
 *     <li>{@code roundCount} - 对话轮次计数（用户消息数量）</li>
 *     <li>{@code title} - 会话标题，自动从第一条消息提取或使用默认值</li>
 *     <li>{@code sysPrompt} - 系统提示词，用于恢复 Agent 的系统上下文</li>
 *     <li>{@code compressedSummary} - 压缩后的历史摘要，用于长对话的上下文管理</li>
 *     <li>{@code compactionCount} - 压缩执行次数，用于监控和优化</li>
 *     <li>{@code summaryUpdatedAt} - 摘要最后更新时间</li>
 *     <li>{@code version} - 乐观锁版本号，防止并发更新冲突</li>
 * </ul>
 *
 * <p>索引策略：</p>
 * <ul>
 *     <li>复合唯一索引 (userId, session_id) 确保每个用户的每个会话只有一条记忆记录</li>
 * </ul>
 *
 * <p>该文档由 {@link com.liangshou.tangdynasty.agentic.application.ConversationPersistenceService} 管理，
 * 支持对话历史的加载、保存、搜索和压缩操作。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations_memory")
@CompoundIndex(name = "uk_memory_user_session", def = "{'userId': 1, 'session_id': 1}", unique = true)
public class ConversationMemoryDocument {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    private String userId;

    @Builder.Default
    private List<StoredMessage> messages = new ArrayList<>();

    private Long roundCount;

    private Instant updatedAt;

    private Instant createdAt;

    @Version
    private Long version;

    private String title;

    @Field("sys_prompt")
    private String sysPrompt;

    @Field("compressed_summary")
    private String compressedSummary;

    @Field("compaction_count")
    private Long compactionCount;

    @Field("summary_updated_at")
    private Instant summaryUpdatedAt;
}
