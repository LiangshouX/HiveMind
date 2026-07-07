package com.liangshou.agentic.infrastructure.mongo.repository;

import com.liangshou.agentic.domain.profile.model.AgentProfileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Agent Profile Repository - MongoDB 数据访问层，操作用户 Profile 配置。
 *
 * <p>该接口提供以下核心功能：</p>
 * <ul>
 *     <li><b>查询</b>：按用户ID查询所有 Profile，或按用户ID+文件名查询单个 Profile</li>
 *     <li><b>存在性检查</b>：检查用户是否已有指定的 Profile 配置</li>
 *     <li><b>删除</b>：删除用户的所有 Profile 配置</li>
 *     <li><b>保存</b>：继承 MongoRepository 的 save 方法，支持新增和更新</li>
 * </ul>
 *
 * <p>索引依赖：</p>
 * <ul>
 *     <li>复合唯一索引 (userId, filename) 定义在 {@link AgentProfileDocument} 类上</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Repository
public interface AgentProfileRepository extends MongoRepository<AgentProfileDocument, String> {

    /**
     * 根据用户ID查询所有 Profile 配置。
     *
     * @param userId 用户标识
     * @return 该用户的所有 Profile 配置列表（SOUL.md, AGENTS.md, PROFILE.md 等）
     */
    List<AgentProfileDocument> findByUserId(String userId);

    /**
     * 根据用户ID和文件名查询单个 Profile 配置。
     *
     * @param userId   用户标识
     * @param filename 文件名（如 SOUL.md, AGENTS.md, PROFILE.md）
     * @return Profile 配置，如果不存在则返回 Optional.empty()
     */
    Optional<AgentProfileDocument> findByUserIdAndFilename(String userId, String filename);

    /**
     * 检查用户是否已有指定的 Profile 配置。
     *
     * @param userId   用户标识
     * @param filename 文件名
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByUserIdAndFilename(String userId, String filename);

    /**
     * 删除用户的所有 Profile 配置。
     *
     * <p><strong>注意：</strong>此操作会删除该用户的所有自定义 Profile 配置，
     * 删除后用户将降级使用 resources/profiles/ 下的默认文件。</p>
     *
     * @param userId 用户标识
     */
    void deleteByUserId(String userId);
}
