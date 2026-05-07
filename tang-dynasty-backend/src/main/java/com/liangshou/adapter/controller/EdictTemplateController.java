package com.liangshou.adapter.controller;

import com.liangshou.tangdynasty.agentic.common.util.SecurityUtils;
import com.liangshou.service.IEdictTemplateService;
import com.liangshou.service.dto.EdictExecuteCommand;
import com.liangshou.service.dto.EdictTemplateCreateCommand;
import com.liangshou.service.dto.EdictTemplateDTO;
import com.liangshou.service.dto.EdictTemplateUpdateCommand;
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
@RequestMapping("/api/agent/edict-template")
public class EdictTemplateController {

    private final IEdictTemplateService edictTemplateService;

    public EdictTemplateController(IEdictTemplateService edictTemplateService) {
        this.edictTemplateService = edictTemplateService;
    }

    /**
     * 获取模板列表（系统内置在前，用户自建在后）。
     */
    @GetMapping
    public ResponseEntity<List<EdictTemplateDTO>> listTemplates() {
        String userId = SecurityUtils.getCurrentUserId();
        List<EdictTemplateDTO> templates = edictTemplateService.listTemplates(userId);
        return ResponseEntity.ok(templates);
    }

    /**
     * 获取单个模板详情。
     */
    @GetMapping("/{templateId}")
    public ResponseEntity<EdictTemplateDTO> getTemplate(@PathVariable String templateId) {
        String userId = SecurityUtils.getCurrentUserId();
        EdictTemplateDTO template = edictTemplateService.getTemplate(templateId, userId);
        return ResponseEntity.ok(template);
    }

    /**
     * 创建用户自建模板。
     */
    @PostMapping
    public ResponseEntity<EdictTemplateDTO> createTemplate(@Valid @RequestBody EdictTemplateCreateCommand command) {
        String userId = SecurityUtils.getCurrentUserId();
        EdictTemplateDTO template = edictTemplateService.createTemplate(userId, command);
        return ResponseEntity.ok(template);
    }

    /**
     * 更新用户自建模板。
     */
    @PutMapping("/{templateId}")
    public ResponseEntity<EdictTemplateDTO> updateTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody EdictTemplateUpdateCommand command) {
        String userId = SecurityUtils.getCurrentUserId();
        EdictTemplateDTO template = edictTemplateService.updateTemplate(templateId, userId, command);
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
            @Valid @RequestBody EdictExecuteCommand command) {
        String userId = SecurityUtils.getCurrentUserId();
        IEdictTemplateService.EdictExecuteResult result = edictTemplateService.executeTemplate(templateId, userId, command);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "sessionId", result.sessionId(),
                "title", result.title(),
                "message", result.message()
        ));
    }
}
