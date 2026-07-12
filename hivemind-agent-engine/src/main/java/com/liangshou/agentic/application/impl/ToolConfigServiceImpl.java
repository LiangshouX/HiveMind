package com.liangshou.agentic.application.impl;

import com.liangshou.agentic.agents.tools.SystemToolRegistry;
import com.liangshou.agentic.agents.tools.SystemToolRegistry.SystemToolDefinition;
import com.liangshou.agentic.application.IToolConfigService;
import com.liangshou.agentic.application.dto.ToolConfigDTO;
import com.liangshou.agentic.domain.tool.model.ToolConfigDocument;
import com.liangshou.agentic.domain.tool.model.ToolConfigUpdateCommand;
import com.liangshou.agentic.infrastructure.mongo.repository.ToolConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具配置服务实现。
 *
 * @author LiangshouX
 */
@Service
@Slf4j
public class ToolConfigServiceImpl implements IToolConfigService {

    private final ToolConfigRepository repository;
    private final SystemToolRegistry systemRegistry;
    private com.liangshou.agentic.common.util.ToolConfigProvider configProvider;

    public ToolConfigServiceImpl(ToolConfigRepository repository, SystemToolRegistry systemRegistry) {
        this.repository = repository;
        this.systemRegistry = systemRegistry;
    }

    /**
     * 注入配置提供者（用于缓存刷新）。
     */
    public void setConfigProvider(com.liangshou.agentic.common.util.ToolConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public List<ToolConfigDTO> listByUserId(String userId) {
        // 查询用户配置
        List<ToolConfigDocument> userConfigs = repository.findByUserId(userId);

        // 如果是新用户，同步系统工具
        if (userConfigs.isEmpty()) {
            log.info("[ToolConfig] 新用户 {}，自动同步系统工具", userId);
            syncSystemTools(userId);
            userConfigs = repository.findByUserId(userId);
        }

        return userConfigs.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ToolConfigDTO getToolConfig(String userId, String toolName) {
        return repository.findByUserIdAndToolName(userId, toolName)
                .map(this::toDTO)
                .orElseGet(() -> {
                    // 返回系统默认
                    SystemToolDefinition sysTool = systemRegistry.getTool(toolName);
                    if (sysTool != null) {
                        return systemToDTO(sysTool);
                    }
                    return null;
                });
    }

    @Override
    public ToolConfigDTO updateToolConfig(String userId, String toolName, ToolConfigUpdateCommand command) {
        // 查询现有配置
        ToolConfigDocument config = repository.findByUserIdAndToolName(userId, toolName)
                .orElseGet(() -> createFromSystemDefault(userId, toolName));

        // 更新字段（仅更新非 null 的字段）
        boolean changed = false;
        if (command.getRiskLevel() != null && command.getRiskLevel() != config.getRiskLevel()) {
            log.info("[ToolConfig] 用户 {} 修改工具 {} 风险等级: {} -> {}",
                    userId, toolName, config.getRiskLevel(), command.getRiskLevel());
            config.setRiskLevel(command.getRiskLevel());
            changed = true;
        }
        if (command.getEnabled() != null && command.getEnabled() != config.getEnabled()) {
            log.info("[ToolConfig] 用户 {} {}工具 {}",
                    userId, command.getEnabled() ? "启用" : "禁用", toolName);
            config.setEnabled(command.getEnabled());
            changed = true;
        }
        if (command.getApprovalRequired() != null && command.getApprovalRequired() != config.getApprovalRequired()) {
            log.info("[ToolConfig] 用户 {} 修改工具 {} 审批要求: {} -> {}",
                    userId, toolName, config.getApprovalRequired(), command.getApprovalRequired());
            config.setApprovalRequired(command.getApprovalRequired());
            changed = true;
        }
        if (command.getDenyPatterns() != null) {
            config.setDenyPatterns(command.getDenyPatterns());
            changed = true;
        }

        if (changed) {
            config.setCustomized(true);
            config.setUpdatedAt(Instant.now());
            // 刷新缓存
            if (configProvider != null) {
                configProvider.refreshCache(userId);
            }
        }

        return toDTO(repository.save(config));
    }

    @Override
    public int syncSystemTools(String userId) {
        // 获取系统工具
        Collection<SystemToolDefinition> systemTools = systemRegistry.getAllTools();

        // 查询用户已有工具名称
        List<ToolConfigDocument> existingConfigs = repository.findByUserId(userId);
        Map<String, ToolConfigDocument> existingMap = existingConfigs.stream()
                .collect(Collectors.toMap(ToolConfigDocument::getToolName, doc -> doc));

        // 找出新增工具
        List<ToolConfigDocument> newConfigs = new ArrayList<>();
        for (SystemToolDefinition sysTool : systemTools) {
            if (!existingMap.containsKey(sysTool.getToolName())) {
                newConfigs.add(createFromSystemDefinition(userId, sysTool));
            }
        }

        // 批量保存新增工具
        if (!newConfigs.isEmpty()) {
            repository.saveAll(newConfigs);
            log.info("[ToolConfig] 用户 {} 同步 {} 个新工具", userId, newConfigs.size());
        }

        // 回填已有工具缺失的元数据（category、runEnvironment、description、examples）
        List<ToolConfigDocument> backfillConfigs = new ArrayList<>();
        for (SystemToolDefinition sysTool : systemTools) {
            ToolConfigDocument existing = existingMap.get(sysTool.getToolName());
            if (existing == null) continue;

            boolean needsUpdate = false;
            if (existing.getCategory() == null && sysTool.getCategory() != null) {
                existing.setCategory(sysTool.getCategory());
                needsUpdate = true;
            }
            if (existing.getRunEnvironment() == null && sysTool.getRunEnvironment() != null) {
                existing.setRunEnvironment(sysTool.getRunEnvironment());
                needsUpdate = true;
            }
            if ((existing.getDescription() == null || existing.getDescription().isEmpty())
                    && sysTool.getDescription() != null) {
                existing.setDescription(sysTool.getDescription());
                needsUpdate = true;
            }
            if ((existing.getExamples() == null || existing.getExamples().isEmpty())
                    && sysTool.getExamples() != null && !sysTool.getExamples().isEmpty()) {
                existing.setExamples(sysTool.getExamples());
                needsUpdate = true;
            }

            if (needsUpdate) {
                existing.setUpdatedAt(Instant.now());
                backfillConfigs.add(existing);
            }
        }

        if (!backfillConfigs.isEmpty()) {
            repository.saveAll(backfillConfigs);
            log.info("[ToolConfig] 用户 {} 回填 {} 个工具的元数据", userId, backfillConfigs.size());
        }

        int totalSynced = newConfigs.size() + backfillConfigs.size();
        if (totalSynced > 0 && configProvider != null) {
            configProvider.refreshCache(userId);
        }

        return newConfigs.size();
    }

    @Override
    public ToolConfigDTO getEffectiveConfig(String toolName, String userId) {
        // 优先查询用户配置
        return repository.findByUserIdAndToolName(userId, toolName)
                .map(this::toDTO)
                .orElseGet(() -> {
                    // 降级为系统默认
                    SystemToolDefinition sysTool = systemRegistry.getTool(toolName);
                    if (sysTool != null) {
                        return systemToDTO(sysTool);
                    }
                    // 未知工具，返回低风险的默认配置
                    log.warn("[ToolConfig] 未知工具: {}, 用户: {}", toolName, userId);
                    return ToolConfigDTO.builder()
                            .toolName(toolName)
                            .riskLevel(com.liangshou.agentic.domain.shared.enums.ToolRiskLevel.LOW)
                            .enabled(true)
                            .approvalRequired(false)
                            .denyPatterns(List.of())
                            .examples(List.of())
                            .customized(false)
                            .build();
                });
    }

    /**
     * 从系统默认配置创建用户配置。
     */
    private ToolConfigDocument createFromSystemDefault(String userId, String toolName) {
        SystemToolDefinition sysTool = systemRegistry.getTool(toolName);
        if (sysTool == null) {
            throw new IllegalArgumentException("未知系统工具: " + toolName);
        }
        return createFromSystemDefinition(userId, sysTool);
    }

    /**
     * 从系统工具定义创建用户配置。
     */
    private ToolConfigDocument createFromSystemDefinition(String userId, SystemToolDefinition sysTool) {
        Instant now = Instant.now();
        return ToolConfigDocument.builder()
                .userId(userId)
                .toolName(sysTool.getToolName())
                .riskLevel(sysTool.getRiskLevel())
                .enabled(sysTool.getEnabled())
                .description(sysTool.getDescription())
                .category(sysTool.getCategory())
                .runEnvironment(sysTool.getRunEnvironment())
                .examples(sysTool.getExamples())
                .approvalRequired(sysTool.getApprovalRequired())
                .denyPatterns(sysTool.getDenyPatterns())
                .customized(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 将 Document 转换为 DTO。
     */
    private ToolConfigDTO toDTO(ToolConfigDocument doc) {
        return ToolConfigDTO.builder()
                .toolName(doc.getToolName())
                .description(doc.getDescription())
                .category(doc.getCategory())
                .runEnvironment(doc.getRunEnvironment())
                .riskLevel(doc.getRiskLevel())
                .enabled(doc.getEnabled())
                .approvalRequired(doc.getApprovalRequired())
                .denyPatterns(doc.getDenyPatterns())
                .examples(doc.getExamples())
                .customized(doc.getCustomized())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    /**
     * 将系统工具定义转换为 DTO。
     */
    private ToolConfigDTO systemToDTO(SystemToolDefinition sysTool) {
        return ToolConfigDTO.builder()
                .toolName(sysTool.getToolName())
                .description(sysTool.getDescription())
                .category(sysTool.getCategory())
                .runEnvironment(sysTool.getRunEnvironment())
                .riskLevel(sysTool.getRiskLevel())
                .enabled(sysTool.getEnabled())
                .approvalRequired(sysTool.getApprovalRequired())
                .denyPatterns(sysTool.getDenyPatterns())
                .examples(sysTool.getExamples())
                .customized(false)
                .updatedAt(null)
                .build();
    }
}
