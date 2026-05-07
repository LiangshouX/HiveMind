package com.liangshou.service;

import com.liangshou.service.dto.EdictExecuteCommand;
import com.liangshou.service.dto.EdictTemplateCreateCommand;
import com.liangshou.service.dto.EdictTemplateDTO;
import com.liangshou.service.dto.EdictTemplateUpdateCommand;

import java.util.List;

/**
 * 任务模板服务接口。
 *
 * @author LiangshouX
 */
public interface IEdictTemplateService {

    /**
     * 获取模板列表（系统内置在前，用户自建在后）。
     *
     * @param userId 用户ID（可为空，为空时只返回系统内置）
     * @return 模板列表
     */
    List<EdictTemplateDTO> listTemplates(String userId);

    /**
     * 获取单个模板详情。
     *
     * @param templateId 模板ID
     * @param userId     用户ID（用于校验权限）
     * @return 模板详情
     */
    EdictTemplateDTO getTemplate(String templateId, String userId);

    /**
     * 创建用户自建模板。
     *
     * @param userId  用户ID
     * @param command 创建命令
     * @return 创建后的模板
     */
    EdictTemplateDTO createTemplate(String userId, EdictTemplateCreateCommand command);

    /**
     * 更新用户自建模板。
     *
     * @param templateId 模板ID
     * @param userId     用户ID（用于校验权限）
     * @param command    更新命令
     * @return 更新后的模板
     */
    EdictTemplateDTO updateTemplate(String templateId, String userId, EdictTemplateUpdateCommand command);

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
    EdictExecuteResult executeTemplate(String templateId, String userId, EdictExecuteCommand command);

    /**
     * 执行模板结果。
     */
    record EdictExecuteResult(
            String sessionId,
            String title,
            String message
    ) {}
}
