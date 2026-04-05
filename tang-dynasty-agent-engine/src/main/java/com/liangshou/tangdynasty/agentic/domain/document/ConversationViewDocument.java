package com.liangshou.tangdynasty.agentic.domain.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * ConversationViewDocument 组件，用于存储会话视图信息。
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations_view")
@CompoundIndex(name = "uk_view_user_session", def = "{'userId': 1, 'session_id': 1}", unique = true)
public class ConversationViewDocument {

    @Id
    private String id;

    @Field("session_id")
    private String sessionId;

    private String userId;

    private String title;

    private Instant lastMessageAt;

    private Instant updatedAt;

    private Instant createdAt;

    private long unreadCount;

    private long messageCount;

    @Version
    private Long version;
}
