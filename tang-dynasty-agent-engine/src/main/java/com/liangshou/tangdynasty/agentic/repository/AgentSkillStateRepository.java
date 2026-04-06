package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.domain.document.AgentSkillStateDocument;
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
     * 按用户列出状态。
     *
     * @param userId 用户标识
     * @return 返回结果
     */
    List<AgentSkillStateDocument> findByUserId(String userId);

    /**
     * 查询指定 Skill 状态。
     *
     * @param userId    用户标识
     * @param skillName skill 名称
     * @return 返回结果
     */
    Optional<AgentSkillStateDocument> findByUserIdAndSkillName(String userId, String skillName);

    /**
     * 删除指定 Skill 状态。
     *
     * @param userId    用户标识
     * @param skillName skill 名称
     */
    void deleteByUserIdAndSkillName(String userId, String skillName);
}
