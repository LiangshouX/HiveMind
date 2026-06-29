package com.liangshou.service;

import com.liangshou.service.dto.TaskExecuteCommand;
import com.liangshou.service.dto.TaskTemplateCreateCommand;
import com.liangshou.service.dto.TaskTemplateDTO;
import com.liangshou.service.dto.TaskTemplateUpdateCommand;

import java.util.List;

/**
 * 任务模板服务接口。
 *
 * @author LiangshouX
 */
public interface ITaskTemplateService {

    /**
     * 获取模板列表（系统内置在前，用户自建在后）。
     *
     * @param userId 用户ID（可为空，为空时只返回系统内置）
     * @return 模板列表
     */
    List<TaskTemplateDTO> listTemplates(String userId);

    /**
     * 获取单个模板详情。
     *
     * @param templateId 模板ID
     * @param userId     用户ID（用于校验权限）
     * @return 模板详情
     */
    TaskTemplateDTO getTemplate(String templateId, String userId);

    /**
     * 创建用户自建模板。
     *
     * @param userId  用户ID
     * @param command 创建命令
     * @return 创建后的模板
     */
    TaskTemplateDTO createTemplate(String userId, TaskTemplateCreateCommand command);

    /**
     * 更新用户自建模板。
     *
     * @param templateId 模板ID
     * @param userId     用户ID（用于校验权限）
     * @param command    更新命令
     * @return 更新后的模板
     */
    TaskTemplateDTO updateTemplate(String templateId, String userId, TaskTemplateUpdateCommand command);

    /**
     * 删除用户自建模板。
     *
     * @param templateId 模板ID
     * @param userId     用户ID（用于校验权限）
     */
    void deleteTemplate(String templateId, String userId);

    /**
     * 下旨（执行模板）。
     *
     * @param templateId 模板ID
     * @param userId     用户ID
     * @param command    执行参数
     * @return 执行结果（含 sessionId）
     */
    TaskExecuteResult executeTemplate(String templateId, String userId, TaskExecuteCommand command);

    /**
     * 执行模板结果。
     */
    record TaskExecuteResult(
            String sessionId,
            String title,
            String message
    ) {}
}
