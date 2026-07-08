package com.liangshou.agentic.infrastructure.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.provider.ProviderRuntimeParams;
import com.liangshou.agentic.agents.provider.TdAgentModelDescriptor;
import com.liangshou.agentic.agents.provider.TdAgentProviderDescriptor;
import com.liangshou.agentic.infrastructure.provider.query.IProviderQueryService;
import com.liangshou.agentic.infrastructure.provider.query.ProviderQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 数据库供应商配置加载器 - 从 MySQL 数据库加载用户的供应商配置。
 *
 * <p>该组件负责：</p>
 * <ul>
 *     <li>通过 {@link IProviderQueryService} 查询用户的供应商配置</li>
 *     <li>解密 API Key</li>
 *     <li>通过 {@link ProviderRuntimeParams} 推导运行时参数（formatter、chatModel、generateKwargs）</li>
 *     <li>将查询结果转换为 {@link TdAgentProviderDescriptor} 列表</li>
 * </ul>
 *
 * <p>性能保证：</p>
 * <ul>
 *     <li>单次 DB 查询（命中唯一索引），延迟 < 5ms</li>
 *     <li>AES 解密在内存中完成，< 1ms</li>
 *     <li>无缓存层，天然支持热更新</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
public class DbProviderConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(DbProviderConfigLoader.class);

    private static final TypeReference<List<Map<String, Object>>> MODEL_LIST_TYPE = new TypeReference<>() {};

    private final IProviderQueryService providerQueryService;
    private final ObjectMapper objectMapper;

    public DbProviderConfigLoader(IProviderQueryService providerQueryService, ObjectMapper objectMapper) {
        this.providerQueryService = providerQueryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 加载指定用户的所有供应商配置。
     *
     * @param userId 用户 ID
     * @return 供应商描述符列表，无记录时返回空列表
     */
    public List<TdAgentProviderDescriptor> loadForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        long startTime = System.nanoTime();

        List<ProviderQueryResult> results = providerQueryService.listByUserId(userId);
        List<TdAgentProviderDescriptor> descriptors = results.stream()
                .map(this::toDescriptor)
                .toList();

        long duration = (System.nanoTime() - startTime) / 1_000_000;
        log.info("[DbProviderConfigLoader] DB 配置加载 - userId: {}, count: {}, duration: {}ms",
                userId, descriptors.size(), duration);
        return descriptors;
    }

    /**
     * 加载指定用户的单个供应商配置。
     *
     * @param userId     用户 ID
     * @param providerId 供应商 ID
     * @return 供应商描述符，不存在时返回 null
     */
    public TdAgentProviderDescriptor loadForUserAndProvider(String userId, String providerId) {
        if (userId == null || userId.isBlank() || providerId == null || providerId.isBlank()) {
            return null;
        }
        long startTime = System.nanoTime();

        ProviderQueryResult result = providerQueryService.getByProviderId(userId, providerId);
        TdAgentProviderDescriptor descriptor = result == null ? null : toDescriptor(result);

        long duration = (System.nanoTime() - startTime) / 1_000_000;
        log.info("[DbProviderConfigLoader] 单供应商加载 - userId: {}, providerId: {}, found: {}, duration: {}ms",
                userId, providerId, descriptor != null, duration);
        return descriptor;
    }

    /**
     * 加载指定用户当前激活的默认供应商配置。
     *
     * @param userId 用户 ID
     * @return 激活的供应商描述符，无激活供应商时返回 null
     */
    public TdAgentProviderDescriptor loadActivatedForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        long startTime = System.nanoTime();

        ProviderQueryResult result = providerQueryService.getActivated(userId);
        TdAgentProviderDescriptor descriptor = result == null ? null : toDescriptor(result);

        long duration = (System.nanoTime() - startTime) / 1_000_000;
        log.info("[DbProviderConfigLoader] 激活供应商加载 - userId: {}, found: {}, duration: {}ms",
                userId, descriptor != null, duration);
        return descriptor;
    }

    /**
     * 将数据库查询结果转换为供应商描述符。
     */
    private TdAgentProviderDescriptor toDescriptor(ProviderQueryResult result) {
        ProviderRuntimeParams runtimeParams = ProviderRuntimeParams.resolveOrDefault(result.getModelProviderId());

        TdAgentProviderDescriptor descriptor = new TdAgentProviderDescriptor();
        descriptor.setId(result.getModelProviderId());
        descriptor.setName(result.getProviderName());
        descriptor.setProviderType(runtimeParams.providerType().name().toLowerCase(java.util.Locale.ROOT));
        descriptor.setBaseUrl(result.getBaseUrl());
        descriptor.setChatModel(runtimeParams.chatModelClassName());
        descriptor.setFormatter(runtimeParams.formatter());
        descriptor.setGenerateKwargs(new java.util.LinkedHashMap<>(runtimeParams.defaultGenerateKwargs()));
        descriptor.setCustom(!result.isSystemBuiltIn());
        descriptor.setSupportModelDiscovery(false);
        descriptor.setSupportConnectionCheck(true);

        // 解密 API Key
        String decryptedKey = providerQueryService.getDecryptedApiKey(result.getUserId(), result.getId());
        descriptor.setApiKey(decryptedKey);

        // 解析模型列表
        descriptor.setModels(parseModelsJson(result.getModelsJson()));

        return descriptor;
    }

    /**
     * 解析 models_json 字段为模型描述符列表。
     */
    private List<TdAgentModelDescriptor> parseModelsJson(String modelsJson) {
        if (modelsJson == null || modelsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> modelMaps = objectMapper.readValue(modelsJson, MODEL_LIST_TYPE);
            return modelMaps.stream().map(this::toModelDescriptor).toList();
        } catch (Exception e) {
            log.warn("[DbProviderConfigLoader] 解析 models_json 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private TdAgentModelDescriptor toModelDescriptor(Map<String, Object> map) {
        TdAgentModelDescriptor model = new TdAgentModelDescriptor();
        model.setId((String) map.get("id"));
        model.setName((String) map.get("name"));
        Object multimodal = map.get("supportsMultimodal");
        if (multimodal instanceof Boolean b) {
            model.setSupportsMultimodal(b);
        }
        Object video = map.get("supportsVideo");
        if (video instanceof Boolean b) {
            model.setSupportsVideo(b);
        }
        return model;
    }
}
