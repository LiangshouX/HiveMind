package com.liangshou.tangdynasty.agentic.infrastructure.mongo.repository;

import com.liangshou.tangdynasty.agentic.domain.tool.model.ToolConfigDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 工具配置存储库 - 提供 ToolConfigDocument 的 MongoDB 数据访问接口。
 *
 * <p>该 Repository 继承自 Spring Data MongoDB 的 MongoRepository，提供：</p>
 * <ul>
 *     <li>标准的 CRUD 操作（保存、查询、删除等）</li>
 *     <li>{@link #findByUserId} - 查询用户的所有工具配置</li>
 *     <li>{@link #findByUserIdAndToolName} - 查询用户的单个工具配置</li>
 *     <li>{@link #findByUserIdAndToolNameIn} - 批量查询用户的多个工具配置</li>
 *     <li>{@link #findByUserIdAndCustomizedFalse} - 查询用户未自定义的工具（用于同步）</li>
 * </ul>
 *
 * @author LiangshouX
 */
public interface ToolConfigRepository extends MongoRepository<ToolConfigDocument, String> {

    /**
     * 查询用户的所有工具配置。
     *
     * @param userId 用户唯一标识
     * @return 该用户的所有工具配置列表；若无配置则返回空列表
     */
    List<ToolConfigDocument> findByUserId(String userId);

    /**
     * 查询用户的单个工具配置。
     *
     * @param userId   用户唯一标识
     * @param toolName 工具名称
     * @return 包含工具配置的 Optional，若不存在则返回 empty
     */
    Optional<ToolConfigDocument> findByUserIdAndToolName(String userId, String toolName);

    /**
     * 批量查询用户的多个工具配置。
     *
     * @param userId     用户唯一标识
     * @param toolNames  工具名称列表
     * @return 匹配的工具配置列表；若无匹配记录则返回空列表
     */
    List<ToolConfigDocument> findByUserIdAndToolNameIn(String userId, List<String> toolNames);

    /**
     * 查询用户未自定义的工具配置（即系统同步的默认配置）。
     *
     * <p>用于同步功能，找出用户尚未自定义的工具，以便添加系统新增的工具。</p>
     *
     * @param userId 用户唯一标识
     * @return 未自定义的工具配置列表
     */
    List<ToolConfigDocument> findByUserIdAndCustomizedFalse(String userId);
}
