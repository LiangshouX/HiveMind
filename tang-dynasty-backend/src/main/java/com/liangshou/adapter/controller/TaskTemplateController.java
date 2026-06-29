package com.liangshou.adapter.controller;

import com.liangshou.agentic.common.util.SecurityUtils;
import com.liangshou.service.ITaskTemplateService;
import com.liangshou.service.dto.TaskExecuteCommand;
import com.liangshou.service.dto.TaskTemplateCreateCommand;
import com.liangshou.service.dto.TaskTemplateDTO;
import com.liangshou.service.dto.TaskTemplateUpdateCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务模板 REST 控制器。
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/agent/task-template")
public class TaskTemplateController {

    private final ITaskTemplateService edictTemplateService;

    public TaskTemplateController(ITaskTemplateService edictTemplateService) {
        this.edictTemplateService = edictTemplateService;
    }

    /**
     * 获取模板列表（系统内置在前，用户自建在后）。
     */
    @GetMapping
    public ResponseEntity<List<TaskTemplateDTO>> listTemplates() {
        String userId = SecurityUtils.getCurrentUserId();
        List<TaskTemplateDTO> templates = edictTemplateService.listTemplates(userId);
        return ResponseEntity.ok(templates);
    }

    /**
     * 获取单个模板详情。
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<TaskTemplateDTO> getTemplate(@PathVariable String templateId) {
        String userId = SecurityUtils.getCurrentUserId();
        TaskTemplateDTO template = edictTemplateService.getTemplate(templateId, userId);
        return ResponseEntity.ok(template);
    }

    /**
     * 创建用户自建模板。
     */
    @PostMapping
    public ResponseEntity<TaskTemplateDTO> createTemplate(@Valid @RequestBody TaskTemplateCreateCommand command) {
        String userId = SecurityUtils.getCurrentUserId();
        TaskTemplateDTO template = edictTemplateService.createTemplate(userId, command);
        return ResponseEntity.ok(template);
    }

    /**
     * 更新用户自建模板。
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<TaskTemplateDTO> updateTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody TaskTemplateUpdateCommand command) {
        String userId = SecurityUtils.getCurrentUserId();
        TaskTemplateDTO template = edictTemplateService.updateTemplate(templateId, userId, command);
        return ResponseEntity.ok(template);
    }

    /**
     * 删除用户自建模板。
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String templateId) {
        String userId = SecurityUtils.getCurrentUserId();
        edictTemplateService.deleteTemplate(templateId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 下旨（执行模板）。
     */
    @PostMapping("/{templateId}/execute")
    public ResponseEntity<Map<String, Object>> executeTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody TaskExecuteCommand command) {
        String userId = SecurityUtils.getCurrentUserId();
        ITaskTemplateService.TaskExecuteResult result = edictTemplateService.executeTemplate(templateId, userId, command);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "sessionId", result.sessionId(),
                "title", result.title(),
                "message", result.message()
        ));
    }
}
