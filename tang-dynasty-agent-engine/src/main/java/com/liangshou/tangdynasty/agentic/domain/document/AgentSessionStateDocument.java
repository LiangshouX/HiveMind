package com.liangshou.tangdynasty.agentic.domain.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * AgentSessionStateDocument 组件，表示会话状态存储的 Mongo Document
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

    private Instant updatedAt;
}
