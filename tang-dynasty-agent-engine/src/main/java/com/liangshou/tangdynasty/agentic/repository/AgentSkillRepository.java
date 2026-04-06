package com.liangshou.tangdynasty.agentic.repository;

import com.liangshou.tangdynasty.agentic.domain.document.AgentSkillDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 自定义 Agent Skill 数据访问接口，提供对 MongoDB 中自定义 Skill 文档的 CRUD 操作。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>按用户查询所有自定义 Skill，按更新时间降序排列</li>
 *   <li>查询指定用户的特定 Skill 定义</li>
 *   <li>删除指定用户的特定 Skill 记录</li>
 * </ul>
 * </p>
 * <p>
 * 该 Repository 继承自 Spring Data MongoDB 的 MongoRepository，
 * 用于管理用户创建的自定义 Skill，包括 Skill 的 Markdown 内容、资源文件等。
 * </p>
 *
 * @author LiangshouX
 */
public interface AgentSkillRepository extends MongoRepository<AgentSkillDocument, String> {

    /**
     * 按用户列出 Skill。
     *
     * @param userId 用户标识
     * @return 返回结果
     */
    List<AgentSkillDocument> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * 查询指定 Skill。
     *
     * @param userId 用户标识
     * @param name   skill 名称
     * @return 返回结果
     */
    Optional<AgentSkillDocument> findByUserIdAndName(String userId, String name);

    /**
     * 删除指定 Skill。
     *
     * @param userId 用户标识
     * @param name   skill 名称
     */
    void deleteByUserIdAndName(String userId, String name);
}
