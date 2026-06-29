package com.liangshou.agentic.domain.profile.model;

import com.liangshou.agentic.domain.profile.enums.ProfileSource;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Agent Profile 文档 - MongoDB 中存储用户自定义 Profile 配置的持久化对象。
 *
 * <p>该文档包含以下核心字段：</p>
 * <ul>
 *     <li>{@code id} - 主键，格式为 "{userId}:{filename}"</li>
 *     <li>{@code userId} - 用户标识</li>
 *     <li>{@code filename} - 文件名（SOUL.md, AGENTS.md, PROFILE.md）</li>
 *     <li>{@code content} - 文件内容（Markdown 格式）</li>
 *     <li>{@code enabled} - 是否启用（对应前端的 active 开关）</li>
 *     <li>{@code source} - 来源（DEFAULT 或 USER_CUSTOMIZED）</li>
 *     <li>{@code version} - 乐观锁版本号，防止并发更新冲突</li>
 *     <li>{@code createdAt} / {@code updatedAt} - 创建和更新时间</li>
 * </ul>
 *
 * <p>索引策略：</p>
 * <ul>
 *     <li>复合唯一索引 (userId, filename) 确保每个用户的每个文件只有一条记录</li>
 * </ul>
 *
 * <p>该文档由 {@link com.liangshou.agentic.application.ITdAgentProfileService} 管理，
 * 支持 Profile 配置的加载、更新和重置操作。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_profiles")
@CompoundIndex(name = "uk_user_filename", def = "{'userId': 1, 'filename': 1}", unique = true)
public class AgentProfileDocument {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("file_name")
    private String filename;

    private String content;

    @Builder.Default
    private boolean enabled = true;

    private ProfileSource source;

    @Version
    private Long version;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}
