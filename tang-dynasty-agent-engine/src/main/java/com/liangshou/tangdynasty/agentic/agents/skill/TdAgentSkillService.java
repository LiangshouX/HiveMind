package com.liangshou.tangdynasty.agentic.agents.skill;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.common.enums.TdAgentSkillSource;
import com.liangshou.tangdynasty.agentic.infrastructure.mongo.domain.document.skill.AgentSkillDocument;
import com.liangshou.tangdynasty.agentic.infrastructure.mongo.domain.document.skill.AgentSkillStateDocument;
import com.liangshou.tangdynasty.agentic.infrastructure.mongo.repository.AgentSkillRepository;
import com.liangshou.tangdynasty.agentic.infrastructure.mongo.repository.AgentSkillStateRepository;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.skill.util.SkillFileSystemHelper;
import io.agentscope.core.skill.util.SkillUtil;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Skill 管理服务，负责 Skill 的完整生命周期管理。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>加载和管理内置 Skills（从 classpath 或文件系统）</li>
 *   <li>创建、更新、删除自定义 Skills</li>
 *   <li>管理 Skill 的启用/禁用状态（支持用户个性化配置）</li>
 *   <li>合并内置和自定义 Skills，处理同名覆盖逻辑</li>
 *   <li>为会话创建 SkillBox，仅包含已启用的 Skills</li>
 *   <li>提供 Skill 列表查询和详情获取接口</li>
 *   <li>验证和规范化 Skill 资源文件路径</li>
 * </ul>
 * </p>
 * <p>
 * 该服务支持两种 Skill 来源：
 * <ul>
 *   <li>BUILTIN - 系统预定义的内置 Skills</li>
 *   <li>CUSTOMIZED - 用户创建的自定义 Skills</li>
 * </ul>
 * 自定义 Skill 可以覆盖同名的内置 Skill，实现个性化扩展。
 * </p>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentSkillService {

    private static final Logger log = LoggerFactory.getLogger(TdAgentSkillService.class);
    private static final String BUILTIN_SOURCE = "builtin";
    private static final String CUSTOMIZED_SOURCE = "customized";

    private final TdAgentProperties properties;
    private final AgentSkillRepository skillRepository;
    private final AgentSkillStateRepository skillStateRepository;
    private volatile Map<String, AgentSkill> builtinSkills = Map.of();

    /**
     * 构造器
     * @param properties 外部化配置
     * @param skillRepository 自定义 skill repository
     * @param skillStateRepository skill 状态 repository
     */
    public TdAgentSkillService(
            TdAgentProperties properties,
            AgentSkillRepository skillRepository,
            AgentSkillStateRepository skillStateRepository) {
        this.properties = properties;
        this.skillRepository = skillRepository;
        this.skillStateRepository = skillStateRepository;
    }

    /**
     * 初始化内置 Skill。
     */
    @PostConstruct
    public void initialize() {
        reloadBuiltinSkills();
    }

    /**
     * 重新加载内置 Skill。
     */
    public synchronized void reloadBuiltinSkills() {
        if (!properties.getSkill().isEnabled()) {
            builtinSkills = Map.of();
            return;
        }
        builtinSkills = Collections.unmodifiableMap(loadBuiltinSkills());
    }

    /**
     * 列出用户可见 Skill。
     * @param userId 用户标识
     * @return 返回结果
     */
    public List<TdAgentSkillInfo> listSkills(String userId) {
        return buildMergedSkillEntries(userId).values().stream()
                .sorted(Comparator.comparing(SkillEntry::name))
                .map(this::toSkillInfo)
                .toList();
    }

    /**
     * 获取单个 Skill。
     * @param userId 用户标识
     * @param skillName skill 名称
     * @return 返回结果
     */
    public TdAgentSkillInfo getSkill(String userId, String skillName) {
        SkillEntry entry = buildMergedSkillEntries(userId).get(skillName);
        if (entry == null) {
            throw new IllegalArgumentException("Skill 不存在: " + skillName);
        }
        return toSkillInfo(entry);
    }

    /**
     * 保存自定义 Skill。
     * @param userId 用户标识
     * @param skillMarkdown SKILL.md 内容
     * @param resources 资源文件
     * @param enabled 是否启用
     * @return 返回结果
     */
    public TdAgentSkillInfo saveCustomSkill(
            String userId, String skillMarkdown, Map<String, String> resources, Boolean enabled) {
        Map<String, String> normalizedResources = normalizeResources(resources);
        AgentSkill skill = SkillUtil.createFrom(skillMarkdown, normalizedResources, CUSTOMIZED_SOURCE);
        Instant now = Instant.now();
        AgentSkillDocument existing = skillRepository.findByUserIdAndName(userId, skill.getName()).orElse(null);
        AgentSkillDocument document =
                AgentSkillDocument.builder()
                        .id(existing != null ? existing.getId() : null)
                        .userId(userId)
                        .name(skill.getName())
                        .description(skill.getDescription())
                        .skillMarkdown(skillMarkdown)
                        .resources(normalizedResources)
                        .createdAt(existing != null ? existing.getCreatedAt() : now)
                        .updatedAt(now)
                        .build();
        skillRepository.save(document);
        upsertSkillState(
                userId,
                skill.getName(),
                enabled != null ? enabled : properties.getSkill().isCustomEnabledByDefault());
        return getSkill(userId, skill.getName());
    }

    /**
     * 更新 Skill 启停状态。
     * @param userId 用户标识
     * @param skillName skill 名称
     * @param enabled 是否启用
     * @return 返回结果
     */
    public TdAgentSkillInfo setSkillEnabled(String userId, String skillName, boolean enabled) {
        SkillEntry entry = buildMergedSkillEntries(userId).get(skillName);
        if (entry == null) {
            throw new IllegalArgumentException("Skill 不存在: " + skillName);
        }
        upsertSkillState(userId, skillName, enabled);
        return getSkill(userId, skillName);
    }

    /**
     * 删除自定义 Skill。
     * @param userId 用户标识
     * @param skillName skill 名称
     */
    public void deleteCustomSkill(String userId, String skillName) {
        AgentSkillDocument document =
                skillRepository.findByUserIdAndName(userId, skillName)
                        .orElseThrow(() -> new IllegalArgumentException("自定义 Skill 不存在: " + skillName));
        skillRepository.deleteByUserIdAndName(userId, document.getName());
        skillStateRepository.deleteByUserIdAndSkillName(userId, document.getName());
    }

    /**
     * 构建当前会话的 SkillBox。
     * @param context 会话上下文
     * @param toolkit 工具集
     * @return 返回结果
     */
    public SkillBox createSkillBox(ConversationSessionContext context, Toolkit toolkit) {
        if (!properties.getSkill().isEnabled()) {
            return null;
        }
        List<SkillEntry> activeSkills = buildMergedSkillEntries(context.getUserId()).values().stream()
                .filter(SkillEntry::enabled)
                .sorted(Comparator.comparing(SkillEntry::name))
                .toList();
        if (activeSkills.isEmpty()) {
            return null;
        }
        SkillBox skillBox = new SkillBox(toolkit);
        for (SkillEntry entry : activeSkills) {
            skillBox.registration().skill(entry.agentSkill()).apply();
        }
        return skillBox;
    }

    private Map<String, SkillEntry> buildMergedSkillEntries(String userId) {
        Map<String, Boolean> stateMap =
                skillStateRepository.findByUserId(userId).stream()
                        .filter(document -> document.getEnabled() != null)
                        .collect(
                                Collectors.toMap(
                                        AgentSkillStateDocument::getSkillName,
                                        AgentSkillStateDocument::getEnabled,
                                        (left, right) -> right));
        Map<String, SkillEntry> merged = new LinkedHashMap<>();
        for (AgentSkill builtinSkill : builtinSkills.values()) {
            merged.put(
                    builtinSkill.getName(),
                    new SkillEntry(
                            builtinSkill,
                            toSkillMarkdown(builtinSkill),
                            TdAgentSkillSource.BUILTIN,
                            stateMap.getOrDefault(
                                    builtinSkill.getName(),
                                    properties.getSkill().isBuiltinEnabledByDefault()),
                            false,
                            null,
                            userId));
        }
        for (AgentSkillDocument document : skillRepository.findByUserIdOrderByUpdatedAtDesc(userId)) {
            try {
                AgentSkill customSkill =
                        SkillUtil.createFrom(
                                document.getSkillMarkdown(),
                                normalizeResources(document.getResources()),
                                CUSTOMIZED_SOURCE);
                merged.put(
                        customSkill.getName(),
                        new SkillEntry(
                                customSkill,
                                document.getSkillMarkdown(),
                                TdAgentSkillSource.CUSTOMIZED,
                                stateMap.getOrDefault(
                                        customSkill.getName(),
                                        properties.getSkill().isCustomEnabledByDefault()),
                                builtinSkills.containsKey(customSkill.getName()),
                                document.getUpdatedAt(),
                                userId));
            } catch (RuntimeException ex) {
                log.warn("加载自定义 Skill 失败，已跳过: userId={}, skill={}", userId, document.getName(), ex);
            }
        }
        return merged;
    }

    private TdAgentSkillInfo toSkillInfo(SkillEntry entry) {
        return TdAgentSkillInfo.builder()
                .userId(entry.userId())
                .name(entry.name())
                .description(entry.agentSkill().getDescription())
                .skillMarkdown(entry.skillMarkdown())
                .resources(entry.agentSkill().getResources())
                .source(entry.source())
                .enabled(entry.enabled())
                .overriddenBuiltin(entry.overriddenBuiltin())
                .updatedAt(entry.updatedAt())
                .build();
    }

    private Map<String, AgentSkill> loadBuiltinSkills() {
        String location = properties.getSkill().getBuiltinLocation();
        if (location == null || location.isBlank()) {
            return Map.of();
        }
        List<AgentSkill> skills = location.startsWith("classpath:")
                ? loadFromClasspath(location.substring("classpath:".length()))
                : loadFromFileSystem(Path.of(location));
        Map<String, AgentSkill> loaded = new LinkedHashMap<>();
        skills.stream().sorted(Comparator.comparing(AgentSkill::getName)).forEach(skill -> loaded.put(skill.getName(), skill));
        return loaded;
    }

    private List<AgentSkill> loadFromClasspath(String location) {
        String classpathRoot = trimLeadingSlash(location);
        try (ClasspathSkillRepository repository = new ClasspathSkillRepository(classpathRoot)) {
            return new ArrayList<>(repository.getAllSkills());
        } catch (IOException ex) {
            throw new IllegalStateException("加载内置 Skill 失败: " + classpathRoot, ex);
        }
    }

    private List<AgentSkill> loadFromFileSystem(Path location) {
        if (!Files.exists(location)) {
            throw new IllegalStateException("Skill 目录不存在: " + location);
        }
        return SkillFileSystemHelper.getAllSkills(location, BUILTIN_SOURCE);
    }

    private void upsertSkillState(String userId, String skillName, boolean enabled) {
        AgentSkillStateDocument existing =
                skillStateRepository.findByUserIdAndSkillName(userId, skillName).orElse(null);
        AgentSkillStateDocument document =
                AgentSkillStateDocument.builder()
                        .id(existing != null ? existing.getId() : null)
                        .userId(userId)
                        .skillName(skillName)
                        .enabled(enabled)
                        .updatedAt(Instant.now())
                        .build();
        skillStateRepository.save(document);
    }

    private Map<String, String> normalizeResources(Map<String, String> resources) {
        if (resources == null || resources.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            String resourcePath = normalizeResourcePath(entry.getKey());
            String content = Objects.requireNonNull(entry.getValue(), "Skill 资源内容不能为空。");
            normalized.put(resourcePath, content);
        }
        return normalized;
    }

    private String normalizeResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("Skill 资源路径不能为空。");
        }
        String normalized = resourcePath.replace('\\', '/').trim();
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new IllegalArgumentException("Skill 资源路径不允许越级或绝对路径。");
        }
        return normalized;
    }

    private String toSkillMarkdown(AgentSkill skill) {
        return """
                ---
                name: %s
                description: %s
                ---

                %s
                """
                .formatted(
                        skill.getName(),
                        escapeYaml(skill.getDescription()),
                        skill.getSkillContent());
    }

    private String escapeYaml(String value) {
        String normalized = Optional.ofNullable(value).orElse("");
        return "\"" + normalized.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String trimLeadingSlash(String location) {
        String normalized = location.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private record SkillEntry(
            AgentSkill agentSkill,
            String skillMarkdown,
            TdAgentSkillSource source,
            boolean enabled,
            boolean overriddenBuiltin,
            Instant updatedAt,
            String userId) {

        private String name() {
            return agentSkill.getName();
        }
    }
}
