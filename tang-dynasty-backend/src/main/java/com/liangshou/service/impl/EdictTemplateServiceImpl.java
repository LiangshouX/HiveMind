package com.liangshou.service.impl;

import com.liangshou.tangdynasty.agentic.application.ITdAgentChatService;
import com.liangshou.tangdynasty.agentic.application.dto.ChatRequest;
import com.liangshou.tangdynasty.agentic.application.dto.ChatResponse;
import com.liangshou.service.IEdictTemplateService;
import com.liangshou.service.dto.EdictExecuteCommand;
import com.liangshou.service.dto.EdictTemplateCreateCommand;
import com.liangshou.service.dto.EdictTemplateDTO;
import com.liangshou.service.dto.EdictTemplateUpdateCommand;
import com.liangshou.infrastructure.mongo.domain.EdictTemplateDocument;
import com.liangshou.infrastructure.mongo.domain.EdictTemplateDocument.TemplateParam;
import com.liangshou.infrastructure.mongo.domain.EdictTemplateDocument.TemplateType;
import com.liangshou.infrastructure.mongo.repository.EdictTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 任务模板服务实现。
 *
 * @author LiangshouX
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdictTemplateServiceImpl implements IEdictTemplateService {

    private final EdictTemplateRepository templateRepository;
    private final ITdAgentChatService chatService;

    @Override
    public List<EdictTemplateDTO> listTemplates(String userId) {
        List<EdictTemplateDocument> allTemplates = new ArrayList<>();

        // 系统内置模板
        allTemplates.addAll(templateRepository.findByTypeOrderByCategoryAsc(TemplateType.SYSTEM));

        // 用户自建模板
        if (userId != null && !userId.isBlank()) {
            allTemplates.addAll(templateRepository.findByUserIdOrderByCreatedAtDesc(userId));
        }

        return allTemplates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EdictTemplateDTO getTemplate(String templateId, String userId) {
        EdictTemplateDocument doc = templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        // 用户自建模板需要校验权限
        if (doc.getType() == TemplateType.USER) {
            if (userId == null || !userId.equals(doc.getUserId())) {
                throw new IllegalArgumentException("无权访问该模板");
            }
        }

        return toDTO(doc);
    }

    @Override
    public EdictTemplateDTO createTemplate(String userId, EdictTemplateCreateCommand command) {
        String templateId = "user_tpl_" + UUID.randomUUID().toString().substring(0, 8);

        EdictTemplateDocument doc = EdictTemplateDocument.builder()
                .templateId(templateId)
                .name(command.getName())
                .description(command.getDescription())
                .category(command.getCategory())
                .icon(command.getIcon())
                .command(command.getCommand())
                .params(command.getParams())
                .depts(command.getDepts())
                .est(command.getEst())
                .cost(command.getCost())
                .type(TemplateType.USER)
                .userId(userId)
                .createdAt(Instant.now())
                .updatedAt(null)
                .build();

        templateRepository.save(doc);
        log.info("用户 {} 创建模板: {} ({})", userId, command.getName(), templateId);
        return toDTO(doc);
    }

    @Override
    public EdictTemplateDTO updateTemplate(String templateId, String userId, EdictTemplateUpdateCommand command) {
        EdictTemplateDocument doc = templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        // 只能更新用户自建模板
        if (doc.getType() == TemplateType.SYSTEM) {
            throw new IllegalStateException("系统内置模板不可编辑");
        }
        if (userId == null || !userId.equals(doc.getUserId())) {
            throw new IllegalArgumentException("无权编辑该模板");
        }

        // 更新字段
        if (command.getName() != null) doc.setName(command.getName());
        if (command.getDescription() != null) doc.setDescription(command.getDescription());
        if (command.getCategory() != null) doc.setCategory(command.getCategory());
        if (command.getIcon() != null) doc.setIcon(command.getIcon());
        if (command.getCommand() != null) doc.setCommand(command.getCommand());
        if (command.getParams() != null) doc.setParams(command.getParams());
        if (command.getDepts() != null) doc.setDepts(command.getDepts());
        if (command.getEst() != null) doc.setEst(command.getEst());
        if (command.getCost() != null) doc.setCost(command.getCost());
        doc.setUpdatedAt(Instant.now());

        templateRepository.save(doc);
        log.info("用户 {} 更新模板: {} ({})", userId, doc.getName(), templateId);
        return toDTO(doc);
    }

    @Override
    public void deleteTemplate(String templateId, String userId) {
        EdictTemplateDocument doc = templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        if (doc.getType() == TemplateType.SYSTEM) {
            throw new IllegalStateException("系统内置模板不可删除");
        }
        if (userId == null || !userId.equals(doc.getUserId())) {
            throw new IllegalArgumentException("无权删除该模板");
        }

        templateRepository.delete(doc);
        log.info("用户 {} 删除模板: {} ({})", userId, doc.getName(), templateId);
    }

    @Override
    public EdictExecuteResult executeTemplate(String templateId, String userId, EdictExecuteCommand command) {
        // 1. 获取模板
        EdictTemplateDocument doc = templateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        // 2. 替换参数，生成最终命令
        String finalCommand = buildCommand(doc, command.getParams());
        if (finalCommand.isBlank()) {
            throw new IllegalArgumentException("参数替换后命令为空");
        }

        // 3. 生成 session ID
        String sessionId = "sess_" + UUID.randomUUID().toString().substring(0, 12);

        // 4. 调用 Agent 服务发起任务
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setUserId(userId);
        chatRequest.setSessionId(sessionId);
        chatRequest.setMessage(finalCommand);
        chatRequest.setTitle(doc.getName() + " - " + doc.getDepts().stream().findFirst().orElse(""));

        try {
            ChatResponse response = chatService.chat(chatRequest);
            String resultSessionId = response != null && response.getSessionId() != null
                    ? response.getSessionId()
                    : sessionId;

            log.info("用户 {} 下旨模板: {} -> session: {}", userId, doc.getName(), resultSessionId);
            return new EdictExecuteResult(resultSessionId, chatRequest.getTitle(), "旨意已下达");
        } catch (Exception e) {
            log.error("下旨失败: {}", e.getMessage(), e);
            throw new RuntimeException("下旨失败: " + e.getMessage(), e);
        }
    }

    /**
     * 替换模板参数
     */
    private String buildCommand(EdictTemplateDocument doc, java.util.Map<String, String> params) {
        String cmd = doc.getCommand();
        if (doc.getParams() != null) {
            for (TemplateParam p : doc.getParams()) {
                String value = params != null ? params.get(p.getKey()) : null;
                if (value == null || value.isBlank()) {
                    value = p.getDefaultValue();
                }
                if (p.isRequired() && (value == null || value.isBlank())) {
                    throw new IllegalArgumentException("缺少必填参数: " + p.getLabel());
                }
                cmd = cmd.replace("{" + p.getKey() + "}", value != null ? value : "");
            }
        }
        return cmd;
    }

    /**
     * Document -> DTO 转换
     */
    private EdictTemplateDTO toDTO(EdictTemplateDocument doc) {
        return EdictTemplateDTO.builder()
                .templateId(doc.getTemplateId())
                .name(doc.getName())
                .description(doc.getDescription())
                .category(doc.getCategory())
                .icon(doc.getIcon())
                .command(doc.getCommand())
                .params(doc.getParams())
                .depts(doc.getDepts())
                .est(doc.getEst())
                .cost(doc.getCost())
                .type(doc.getType())
                .userId(doc.getUserId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
