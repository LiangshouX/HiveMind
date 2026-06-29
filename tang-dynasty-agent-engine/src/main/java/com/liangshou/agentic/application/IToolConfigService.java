package com.liangshou.agentic.application;

import com.liangshou.agentic.application.dto.ToolConfigDTO;
import com.liangshou.agentic.domain.tool.model.ToolConfigUpdateCommand;

import java.util.List;

/**
 * 工具配置服务接口 - 提供工具配置的查询、更新和同步功能。
 *
 * @author LiangshouX
 */
public interface IToolConfigService {

    /**
     * 查询用户的所有工具配置。
     *
     * <p>如果是新用户（无配置），自动同步系统默认工具后再返回。</p>
     *
     * @param userId 用户唯一标识
     * @return 用户的所有工具配置列表
     */
    List<ToolConfigDTO> listByUserId(String userId);

    /**
     * 获取单个工具配置详情。
     *
     * @param userId   用户唯一标识
     * @param toolName 工具名称
     * @return 工具配置；若不存在返回 null
     */
    ToolConfigDTO getToolConfig(String userId, String toolName);

    /**
     * 更新工具配置。
     *
     * <p>仅更新命令中非 null 的字段。如果配置不存在，先从系统默认创建。</p>
     *
     * @param userId    用户唯一标识
     * @param toolName  工具名称
     * @param command   更新命令
     * @return 更新后的配置
     */
    ToolConfigDTO updateToolConfig(String userId, String toolName, ToolConfigUpdateCommand command);

    /**
     * 同步系统工具到用户配置。
     *
     * <p>找出系统新增的工具（用户尚未配置），为用户创建默认配置。</p>
     *
     * @param userId 用户唯一标识
     * @return 同步的新增工具数量
     */
    int syncSystemTools(String userId);

    /**
     * 获取有效的工具配置（供 ToolGuardEngine 和 ToolkitFactory 使用）。
     *
     * <p>优先查询用户配置，若不存在则返回系统默认配置。</p>
     *
     * @param toolName 工具名称
     * @param userId   用户唯一标识
     * @return 有效的工具配置
     */
    ToolConfigDTO getEffectiveConfig(String toolName, String userId);
}
