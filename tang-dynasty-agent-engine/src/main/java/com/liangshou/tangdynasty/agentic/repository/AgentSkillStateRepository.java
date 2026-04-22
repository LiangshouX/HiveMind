package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.domain.document.skill.AgentSkillStateDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Agent Skill 启停状态数据访问接口，提供对 MongoDB 中 Skill 状态文档的 CRUD 操作。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>按用户查询所有 Skill 的启用/禁用状态</li>
 *   <li>查询指定用户的特定 Skill 状态</li>
 *   <li>删除指定用户的特定 Skill 状态记录</li>
 * </ul>
 * </p>
 * <p>
 * 该 Repository 继承自 Spring Data MongoDB 的 MongoRepository，
 * 自动提供基础的数据库操作方法，并通过方法名约定实现自定义查询。
 * </p>
 *
 * @author LiangshouX
 */
public interface AgentSkillStateRepository extends MongoRepository<AgentSkillStateDocument, String> {

    /**
     * 查询指定用户的所有 Skill 启用/禁用状态列表。
     *
     * <p>该方法用于获取用户在系统中所有已配置 Skill 的当前状态（启用或禁用），
     * 通常用于初始化用户的 Skill 配置界面或批量检查可用 Skill。</p>
     *
     * @param userId 用户唯一标识
     * @return 该用户的所有 Skill 状态记录列表；若用户无任何 Skill 配置则返回空列表
     */
    List<AgentSkillStateDocument> findByUserId(String userId);

    /**
     * 查询指定用户的特定 Skill 启用/禁用状态。
     *
     * <p>该方法用于精确获取用户对某个具体 Skill 的配置状态，
     * 常用于判断某个 Skill 是否对当前用户可用，或在 Skill 详情页面展示其开关状态。</p>
     *
     * @param userId    用户唯一标识
     * @param skillName Skill 的唯一名称标识
     * @return 包含 Skill 状态记录的 Optional，若该用户未配置此 Skill 则返回 empty
     */
    Optional<AgentSkillStateDocument> findByUserIdAndSkillName(String userId, String skillName);

    /**
     * 删除指定用户的特定 Skill 状态记录。
     *
     * <p>该方法用于移除用户对某个 Skill 的启用/禁用配置，通常在以下场景使用：</p>
     * <ul>
     *     <li>用户卸载或删除自定义 Skill</li>
     *     <li>重置 Skill 配置为系统默认状态</li>
     *     <li>清理无效或过期的 Skill 配置</li>
     * </ul>
     *
     * @param userId    用户唯一标识
     * @param skillName Skill 的唯一名称标识
     */
    void deleteByUserIdAndSkillName(String userId, String skillName);
}
