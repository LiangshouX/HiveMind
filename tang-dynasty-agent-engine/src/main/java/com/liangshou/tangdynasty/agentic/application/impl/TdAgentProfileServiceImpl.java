package com.liangshou.tangdynasty.agentic.application.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liangshou.tangdynasty.agentic.application.ITdAgentProfileService;
import com.liangshou.tangdynasty.agentic.domain.profile.enums.ProfileSource;
import com.liangshou.tangdynasty.agentic.domain.profile.model.AgentProfileDocument;
import com.liangshou.tangdynasty.agentic.infrastructure.mongo.repository.AgentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Agent Profile 服务实现 - 管理用户 Profile 配置的加载、更新和重置。
 *
 * <p>该实现使用 Caffeine 本地缓存优化性能，减少 MongoDB 查询。</p>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentProfileServiceImpl implements ITdAgentProfileService {

    private static final Logger log = LoggerFactory.getLogger(TdAgentProfileServiceImpl.class);

    private static final String PROFILES_PATH = "profiles/";
    private static final List<String> DEFAULT_FILES = List.of("SOUL.md", "AGENTS.md", "PROFILE.md");

    private final AgentProfileRepository profileRepository;

    /**
     * Profile 缓存：userId -> List<AgentProfileDocument>
     * 最大 1000 个用户，5 分钟过期
     */
    private final Cache<String, List<AgentProfileDocument>> profileCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build();

    /**
     * 构造器
     *
     * @param profileRepository Profile Repository
     */
    public TdAgentProfileServiceImpl(AgentProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional
    public void initializeUser(String userId) {
        log.info("开始为用户初始化 Profile 配置 - userId: {}", userId);

        int initializedCount = 0;
        for (String filename : DEFAULT_FILES) {
            // 如果用户已有该 Profile，跳过
            if (profileRepository.existsByUserIdAndFilename(userId, filename)) {
                log.debug("用户已有 Profile 配置，跳过 - userId: {}, filename: {}", userId, filename);
                continue;
            }

            // 读取默认文件内容
            String content = loadDefaultFile(filename);
            if (content == null || content.isBlank()) {
                log.warn("默认 Profile 文件不存在或为空，跳过 - filename: {}", filename);
                continue;
            }

            // 创建 Profile Document
            AgentProfileDocument document = AgentProfileDocument.builder()
                    .id(userId + ":" + filename)
                    .userId(userId)
                    .filename(filename)
                    .content(content)
                    .enabled(true)
                    .source(ProfileSource.DEFAULT)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            profileRepository.save(document);
            initializedCount++;
            log.debug("成功初始化 Profile - userId: {}, filename: {}", userId, filename);
        }

        // 清除缓存（确保下次加载时读取最新数据）
        profileCache.invalidate(userId);

        log.info("用户 Profile 初始化完成 - userId: {}, 初始化数量: {}", userId, initializedCount);
    }

    @Override
    public List<AgentProfileDocument> loadUserProfiles(String userId) {
        // 先查缓存
        List<AgentProfileDocument> cached = profileCache.getIfPresent(userId);
        if (cached != null) {
            log.debug("命中 Profile 缓存 - userId: {}", userId);
            return cached;
        }

        // 缓存未命中，从 MongoDB 加载
        log.debug("未命中 Profile 缓存，从 MongoDB 加载 - userId: {}", userId);
        List<AgentProfileDocument> profiles = loadFromMongo(userId);

        // 写入缓存
        profileCache.put(userId, profiles);

        return profiles;
    }

    @Override
    @Transactional
    public AgentProfileDocument updateProfile(String userId, String filename, String content, boolean enabled) {
        // 校验文件名
        validateFilename(filename);

        log.info("更新 Profile 配置 - userId: {}, filename: {}, enabled: {}", userId, filename, enabled);

        // 查询或创建 Document
        AgentProfileDocument document = profileRepository
                .findByUserIdAndFilename(userId, filename)
                .orElseGet(() -> {
                    log.debug("Profile 不存在，创建新记录 - userId: {}, filename: {}", userId, filename);
                    return AgentProfileDocument.builder()
                            .id(userId + ":" + filename)
                            .userId(userId)
                            .filename(filename)
                            .enabled(true)
                            .source(ProfileSource.USER_CUSTOMIZED)
                            .createdAt(Instant.now())
                            .build();
                });

        // 更新字段
        document.setContent(content);
        document.setEnabled(enabled);
        document.setSource(ProfileSource.USER_CUSTOMIZED);
        document.setUpdatedAt(Instant.now());

        AgentProfileDocument saved = profileRepository.save(document);

        // 清除缓存（热更新生效）
        profileCache.invalidate(userId);
        log.info("Profile 更新成功，缓存已清除 - userId: {}, filename: {}", userId, filename);

        return saved;
    }

    @Override
    @Transactional
    public int batchUpdateProfiles(String userId, List<String> filenames, List<String> contents, List<Boolean> enabled) {
        if (filenames.size() != contents.size() || filenames.size() != enabled.size()) {
            throw new IllegalArgumentException("filenames, contents, enabled 列表大小不一致");
        }

        log.info("批量更新 Profile 配置 - userId: {}, 数量: {}", userId, filenames.size());

        int updatedCount = 0;
        for (int i = 0; i < filenames.size(); i++) {
            try {
                updateProfile(userId, filenames.get(i), contents.get(i), enabled.get(i));
                updatedCount++;
            } catch (Exception e) {
                log.error("批量更新 Profile 失败 - userId: {}, filename: {}, error: {}",
                        userId, filenames.get(i), e.getMessage(), e);
                // 继续处理其他文件
            }
        }

        log.info("批量更新 Profile 完成 - userId: {}, 成功数量: {}", userId, updatedCount);
        return updatedCount;
    }

    @Override
    @Transactional
    public AgentProfileDocument resetProfile(String userId, String filename) {
        validateFilename(filename);

        log.info("重置 Profile 为默认值 - userId: {}, filename: {}", userId, filename);

        // 读取默认文件内容
        String defaultContent = loadDefaultFile(filename);
        if (defaultContent == null || defaultContent.isBlank()) {
            throw new IllegalArgumentException("默认 Profile 文件不存在或为空: " + filename);
        }

        // 更新 MongoDB
        AgentProfileDocument document = profileRepository
                .findByUserIdAndFilename(userId, filename)
                .orElseGet(() -> AgentProfileDocument.builder()
                        .id(userId + ":" + filename)
                        .userId(userId)
                        .filename(filename)
                        .enabled(true)
                        .createdAt(Instant.now())
                        .build());

        document.setContent(defaultContent);
        document.setSource(ProfileSource.DEFAULT);
        document.setUpdatedAt(Instant.now());

        AgentProfileDocument saved = profileRepository.save(document);

        // 清除缓存
        profileCache.invalidate(userId);
        log.info("Profile 重置成功 - userId: {}, filename: {}", userId, filename);

        return saved;
    }

    @Override
    public Optional<AgentProfileDocument> getProfile(String userId, String filename) {
        validateFilename(filename);

        // 先查 MongoDB
        Optional<AgentProfileDocument> mongoProfile = profileRepository
                .findByUserIdAndFilename(userId, filename);

        if (mongoProfile.isPresent()) {
            return mongoProfile;
        }

        // MongoDB 中没有，尝试从 resources 读取默认文件
        String defaultContent = loadDefaultFile(filename);
        if (defaultContent != null && !defaultContent.isBlank()) {
            AgentProfileDocument defaultProfile = AgentProfileDocument.builder()
                    .id("default:" + filename)
                    .userId(userId)
                    .filename(filename)
                    .content(defaultContent)
                    .enabled(true)
                    .source(ProfileSource.DEFAULT)
                    .build();
            return Optional.of(defaultProfile);
        }

        // 都没有，返回空
        return Optional.empty();
    }

    @Override
    public List<AgentProfileDocument> listProfiles(String userId) {
        return profileRepository.findByUserId(userId);
    }

    /**
     * 从 MongoDB 加载用户的所有 Profile 配置。
     * 如果某个文件在 MongoDB 中不存在，则尝试从 resources 读取默认文件。
     */
    private List<AgentProfileDocument> loadFromMongo(String userId) {
        List<AgentProfileDocument> profiles = new ArrayList<>();

        for (String filename : DEFAULT_FILES) {
            Optional<AgentProfileDocument> opt = profileRepository
                    .findByUserIdAndFilename(userId, filename);

            if (opt.isPresent()) {
                profiles.add(opt.get());
            } else {
                // MongoDB 中没有，尝试从 resources 读取
                String defaultContent = loadDefaultFile(filename);
                if (defaultContent != null && !defaultContent.isBlank()) {
                    AgentProfileDocument defaultProfile = AgentProfileDocument.builder()
                            .id("default:" + filename)
                            .userId(userId)
                            .filename(filename)
                            .content(defaultContent)
                            .enabled(true)
                            .source(ProfileSource.DEFAULT)
                            .build();
                    profiles.add(defaultProfile);
                    log.debug("从 resources 加载默认 Profile - userId: {}, filename: {}", userId, filename);
                } else {
                    log.warn("Profile 文件不存在（MongoDB 和 resources 都没有）- userId: {}, filename: {}",
                            userId, filename);
                }
            }
        }

        return profiles;
    }

    /**
     * 从 resources/profiles/ 读取默认文件内容。
     */
    private String loadDefaultFile(String filename) {
        try {
            String path = PROFILES_PATH + filename;
            Resource resource = new PathMatchingResourcePatternResolver()
                    .getResource("classpath:" + path);

            if (!resource.exists()) {
                log.warn("默认 Profile 文件不存在 - path: {}", path);
                return null;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                log.debug("成功加载默认 Profile 文件 - path: {}, size: {}", path, content.length());
                return content;
            }
        } catch (IOException e) {
            log.error("加载默认 Profile 文件失败 - filename: {}, error: {}", filename, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 校验文件名是否合法（只允许 SOUL.md, AGENTS.md, PROFILE.md）。
     */
    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename 不能为空");
        }
        if (!DEFAULT_FILES.contains(filename)) {
            throw new IllegalArgumentException("不合法的文件名: " + filename + "，只允许: " + DEFAULT_FILES);
        }
    }
}
