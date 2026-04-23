package com.liangshou.tangdynasty.agentic.infrastructure.mongo.domain.document.skill;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Agent Skill 启停状态 MongoDB 文档实体，用于存储用户对 Skill 的启用/禁用偏好。
 * <p>
 * 该文档记录每个用户对每个 Skill 的启用状态，支持：
 * <ul>
 *   <li>内置 Skill（BUILTIN）的个性化启用/禁用配置</li>
 *   <li>自定义 Skill（CUSTOMIZED）的启用/禁用管理</li>
 * </ul>
 * </p>
 * <p>
 * 数据库约束：
 * <ul>
 *   <li>集合名称：agent_skill_state</li>
 *   <li>唯一索引：userId + skill_name 组合唯一，确保每个用户对每个 Skill 只有一条状态记录</li>
 * </ul>
 * </p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_skill_state")
@CompoundIndex(name = "uk_skill_state_user_name", def = "{'userId': 1, 'skill_name': 1}", unique = true)
public class AgentSkillStateDocument {

    @Id
    private String id;

    private String userId;

    @Field("skill_name")
    private String skillName;

    private Boolean enabled;

    @Field("updated_at")
    private Instant updatedAt;
}
