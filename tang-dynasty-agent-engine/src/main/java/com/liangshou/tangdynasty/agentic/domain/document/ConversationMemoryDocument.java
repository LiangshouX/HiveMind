package com.liangshou.tangdynasty.agentic.domain.document;

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
 * ConversationMemoryDocument 组件，表示会话历史记录。
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
