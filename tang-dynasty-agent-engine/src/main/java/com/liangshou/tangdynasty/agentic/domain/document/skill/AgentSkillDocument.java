package com.liangshou.tangdynasty.agentic.domain.document.skill;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * 自定义 Agent Skill MongoDB 文档实体，用于存储用户创建的 Skill 定义。
 * <p>
 * 该文档包含完整的 Skill 信息，包括：
 * <ul>
 *   <li>Skill 名称和描述</li>
 *   <li>Skill 的 Markdown 格式定义（SKILL.md 内容）</li>
 *   <li>Skill 依赖的资源文件映射（文件名 -> 文件内容）</li>
 *   <li>创建时间和更新时间戳</li>
 * </ul>
 * </p>
 * <p>
 * 数据库约束：
 * <ul>
 *   <li>集合名称：agent_skills</li>
 *   <li>唯一索引：userId + name 组合唯一，确保每个用户的 Skill 名称不重复</li>
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
@Document(collection = "agent_skills")
@CompoundIndex(name = "uk_skill_user_name", def = "{'userId': 1, 'name': 1}", unique = true)
public class AgentSkillDocument {

    @Id
    private String id;

    private String userId;

    private String name;

    private String description;

    @Field("skill_markdown")
    private String skillMarkdown;

    private Map<String, String> resources;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}
