package com.liangshou.tangdynasty.agentic.domain.memory.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * 对话视图文档 - MongoDB 中存储会话列表展示信息的轻量级对象。
 *
 * <p>该文档用于前端会话列表的快速查询和展示，包含：</p>
 * <ul>
 *     <li>{@code id} - 主键，格式为 "{userId}:{sessionId}"</li>
 *     <li>{@code sessionId} / {@code userId} - 会话和用户的唯一标识</li>
 *     <li>{@code title} - 会话标题，用于在列表中标识该对话</li>
 *     <li>{@code messageCount} - 消息总数，显示对话长度</li>
 *     <li>{@code unreadCount} - 未读消息数，用于提醒用户</li>
 *     <li>{@code lastMessageAt} - 最后一条消息的时间，用于排序</li>
 *     <li>{@code createdAt} / {@code updatedAt} - 创建和更新时间</li>
 *     <li>{@code version} - 乐观锁版本号</li>
 * </ul>
 *
 * <p>与 {@link ConversationMemoryDocument} 的区别：</p>
 * <ul>
 *     <li>View 文档只包含元数据，不包含完整的消息内容，查询效率更高</li>
 *     <li>用于会话列表页面，而 Memory 文档用于加载具体会话的完整历史</li>
 * </ul>
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
