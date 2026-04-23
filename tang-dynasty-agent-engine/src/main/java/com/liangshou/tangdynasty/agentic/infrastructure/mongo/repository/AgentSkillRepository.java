package com.liangshou.tangdynasty.agentic.infrastructure.mongo.repository;

import com.liangshou.tangdynasty.agentic.infrastructure.mongo.domain.document.skill.AgentSkillDocument;
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
     * 查询指定用户的所有自定义 Skill，按更新时间降序排列。
     *
     * <p>该方法用于获取用户创建或导入的所有自定义 Skill 定义，最新更新的 Skill 排在前面，
     * 通常用于展示用户的 Skill 管理列表，方便用户查看最近修改的 Skill。</p>
     *
     * @param userId 用户唯一标识
     * @return 该用户的自定义 Skill 列表，按更新时间从新到旧排序；若无自定义 Skill 则返回空列表
     */
    List<AgentSkillDocument> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * 查询指定用户的特定自定义 Skill 定义。
     *
     * <p>该方法用于精确获取某个自定义 Skill 的完整定义信息，包括 Markdown 内容、资源文件引用等，
     * 通常在以下场景使用：</p>
     * <ul>
     *     <li>加载 Skill 详情页面进行编辑或查看</li>
     *     <li>验证 Skill 名称是否已被占用</li>
     *     <li>在执行前获取 Skill 的完整配置</li>
     * </ul>
     *
     * @param userId 用户唯一标识
     * @param name   Skill 的唯一名称标识
     * @return 包含 Skill 定义文档的 Optional，若该用户无此名称的 Skill 则返回 empty
     */
    Optional<AgentSkillDocument> findByUserIdAndName(String userId, String name);

    /**
     * 删除指定用户的特定自定义 Skill 记录。
     *
     * <p>该方法用于永久移除用户创建的自定义 Skill，包括其 Markdown 内容和相关资源配置，
     * 通常在以下场景使用：</p>
     * <ul>
     *     <li>用户主动删除不再需要的 Skill</li>
     *     <li>清理测试或无效的 Skill 定义</li>
     *     <li>系统管理员执行数据清理操作</li>
     * </ul>
     * <p><strong>注意：</strong>删除操作不可逆，建议在执行前确认该 Skill 未被其他会话引用。</p>
     *
     * @param userId 用户唯一标识
     * @param name   Skill 的唯一名称标识
     */
    void deleteByUserIdAndName(String userId, String name);
}
