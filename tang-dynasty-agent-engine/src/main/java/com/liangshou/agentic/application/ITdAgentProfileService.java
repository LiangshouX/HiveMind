package com.liangshou.agentic.application;

import com.liangshou.agentic.domain.profile.model.AgentProfileDocument;

import java.util.List;
import java.util.Optional;

/**
 * Agent Profile 服务接口 - 管理用户 Profile 配置的加载、更新和重置。
 *
 * <p>该服务提供以下核心功能：</p>
 * <ul>
 *     <li><b>用户初始化</b>：为新用户创建默认 Profile 配置</li>
 *     <li><b>加载配置</b>：优先从 MongoDB 读取用户自定义配置，没有则使用 resources 默认文件</li>
 *     <li><b>更新配置</b>：支持单个和批量更新 Profile 文件</li>
 *     <li><b>重置配置</b>：将 Profile 恢复为 resources 默认文件内容</li>
 * </ul>
 *
 * <p>设计原则：</p>
 * <ul>
 *     <li>所有 Profile 文件都是可选的，不存在不报错</li>
 *     <li>用户隔离：每个用户只能操作自己的 Profile</li>
 *     <li>热更新：更新后立即生效（通过清除缓存实现）</li>
 * </ul>
 *
 * @author LiangshouX
 */
public interface ITdAgentProfileService {

    /**
     * 为新用户初始化默认 Profile 配置。
     *
     * <p>该方法在用户注册时调用，执行以下操作：</p>
     * <ol>
     *     <li>从 resources/profiles/ 读取所有默认文件（SOUL.md, AGENTS.md, PROFILE.md）</li>
     *     <li>为每个文件创建 AgentProfileDocument</li>
     *     <li>批量保存到 MongoDB agent_profiles collection</li>
     * </ol>
     *
     * <p><strong>注意：</strong></p>
     * <ul>
     *     <li>如果用户已有 Profile 配置，则跳过初始化（幂等操作）</li>
     *     <li>如果默认文件不存在，跳过该文件不报错</li>
     *     <li>初始化失败不应阻塞用户注册流程</li>
     * </ul>
     *
     * @param userId 用户标识
     */
    void initializeUser(String userId);

    /**
     * 加载用户的所有 Profile 配置。
     *
     * <p>该方法按以下优先级加载：</p>
     * <ol>
     *     <li>优先从 MongoDB 读取用户自定义配置</li>
     *     <li>如果 MongoDB 中没有某个文件，则从 resources/profiles/ 读取默认文件</li>
     *     <li>如果都没有，跳过该文件不报错</li>
     * </ol>
     *
     * <p>返回的列表包含所有存在的 Profile 文件（SOUL.md, AGENTS.md, PROFILE.md），
     * 按加载顺序排列。</p>
     *
     * @param userId 用户标识
     * @return Profile 配置列表（可能为空列表，如果没有任何配置）
     */
    List<AgentProfileDocument> loadUserProfiles(String userId);

    /**
     * 更新单个 Profile 文件。
     *
     * <p>该方法执行以下操作：</p>
     * <ol>
     *     <li>校验文件名（只允许 SOUL.md, AGENTS.md, PROFILE.md）</li>
     *     <li>查询或创建 AgentProfileDocument</li>
     *     <li>更新 content 和 enabled 字段</li>
     *     <li>更新 source 为 USER_CUSTOMIZED</li>
     *     <li>保存到 MongoDB</li>
     *     <li>清除该用户的 Profile 缓存（热更新生效）</li>
     * </ol>
     *
     * @param userId  用户标识
     * @param filename 文件名
     * @param content  文件内容
     * @param enabled  是否启用
     * @return 更新后的 Profile 配置
     * @throws IllegalArgumentException 如果文件名不合法
     */
    AgentProfileDocument updateProfile(String userId, String filename, String content, boolean enabled);

    /**
     * 批量更新 Profile 文件。
     *
     * <p>该方法循环调用 {@link #updateProfile} 更新每个 Profile 文件，
     * 所有更新在同一个事务中完成。</p>
     *
     * @param userId    用户标识
     * @param filenames 文件名列表
     * @param contents   文件内容列表（与 filenames 一一对应）
     * @param enabled    启用状态列表（与 filenames 一一对应）
     * @return 成功更新的数量
     */
    int batchUpdateProfiles(String userId, List<String> filenames, List<String> contents, List<Boolean> enabled);

    /**
     * 重置 Profile 文件为默认值。
     *
     * <p>该方法执行以下操作：</p>
     * <ol>
     *     <li>从 resources/profiles/ 读取默认文件内容</li>
     *     <li>更新 MongoDB 中的 Profile 配置</li>
     *     <li>将 source 设置为 DEFAULT</li>
     *     <li>清除缓存（热更新生效）</li>
     * </ol>
     *
     * <p><strong>注意：</strong>如果默认文件不存在，抛出 IllegalArgumentException。</p>
     *
     * @param userId   用户标识
     * @param filename 文件名
     * @return 重置后的 Profile 配置
     * @throws IllegalArgumentException 如果默认文件不存在
     */
    AgentProfileDocument resetProfile(String userId, String filename);

    /**
     * 获取单个 Profile 配置。
     *
     * <p>该方法优先从 MongoDB 读取用户自定义配置，
     * 如果 MongoDB 中没有，则从 resources/profiles/ 读取默认文件并包装为 AgentProfileDocument。</p>
     *
     * @param userId   用户标识
     * @param filename 文件名
     * @return Profile 配置，如果不存在则返回 Optional.empty()
     */
    Optional<AgentProfileDocument> getProfile(String userId, String filename);

    /**
     * 获取用户的所有 Profile 列表（仅元数据，不包含内容）。
     *
     * <p>该方法返回用户的 Profile 文件列表，包含文件名、启用状态、来源、大小等信息，
     * 但不包含文件内容（用于列表展示场景）。</p>
     *
     * @param userId 用户标识
     * @return Profile 列表
     */
    List<AgentProfileDocument> listProfiles(String userId);
}
