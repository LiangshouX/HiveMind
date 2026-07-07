package com.liangshou.agentic.agents.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.domain.shared.enums.TdAgentProviderType;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型供应商目录注册表 - 管理内置和自定义的 LLM 供应商配置。
 *
 * <p>该组件负责：</p>
 * <ul>
 *     <li><b>加载供应商目录</b>：从 JSON 配置文件（默认 classpath:provider/builtin_provider.json）中读取供应商列表</li>
 *     <li><b>解析模型配置</b>：根据应用配置合并供应商默认值和运行时覆盖值，生成最终的模型配置</li>
 *     <li><b>环境变量替换</b>：支持在配置中使用 {{env:VAR_NAME}} 占位符引用环境变量</li>
 *     <li><b>自动重载</b>：当配置文件发生变化时，支持自动或手动重新加载目录</li>
 *     <li><b>配置验证</b>：验证供应商 ID、类型等必填字段，检测重复配置</li>
 * </ul>
 *
 * <p>配置解析优先级：</p>
 * <ol>
 *     <li>应用配置 (application.yaml) 中的值优先</li>
 *     <li>其次使用供应商配置文件中的默认值</li>
 *     <li>API Key 必须提供，否则抛出异常</li>
 * </ol>
 *
 * <p>支持的供应商类型由 {@link com.liangshou.agentic.domain.shared.enums.TdAgentProviderType} 定义，
 * 目前包括 DASHSCOPE 和 OPENAI。</p>
 *
 * @author LiangshouX
 */
@Component
@SuppressWarnings("unused")
public class TdAgentProviderRegistry {

    private static final TypeReference<List<TdAgentProviderDescriptor>> PROVIDER_LIST_TYPE =
            new TypeReference<>() {};

    private final TdAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final AtomicReference<ProviderCatalogSnapshot> snapshotRef = new AtomicReference<>();

    /**
     * 创建模型供应商目录注册表。
     *
     * @param properties 外部化配置
     * @param objectMapper Jackson 对象映射器
     * @param resourceLoader Spring 资源加载器
     */
    public TdAgentProviderRegistry(
            TdAgentProperties properties, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 在应用启动时加载一次内置模型供应商目录。
     */
    @PostConstruct
    public void initialize() {
        loadSnapshot(true);
    }

    /**
     * 返回当前生效的模型配置，并在需要时自动重载目录文件。
     *
     * @return 已解析的模型配置
     */
    public TdAgentResolvedModelConfig resolveConfiguredModel() {
        ProviderCatalogSnapshot snapshot = currentSnapshot();
        TdAgentProperties.Model modelProperties = properties.getModel();
        String providerId = normalize(modelProperties.getProviderId(), "dashscope");
        TdAgentProviderDescriptor provider = snapshot.providersById().get(providerId);
        if (provider == null) {
            throw new IllegalStateException("未找到内置模型供应商: " + providerId);
        }
        String configuredModelId =
                firstNonBlank(modelProperties.getModelId(), modelProperties.getModelName());
        TdAgentModelDescriptor model = resolveModel(provider, configuredModelId);
        String apiKey = firstNonBlank(modelProperties.getApiKey(), provider.getApiKey());
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("模型 API Key 未配置，当前供应商: " + provider.getId());
        }
        return TdAgentResolvedModelConfig.builder()
                .providerId(provider.getId())
                .providerName(provider.getName())
                .providerType(TdAgentProviderType.fromValue(provider.getProviderType()))
                .modelId(model.getId())
                .modelName(model.getName())
                .apiKey(apiKey)
                .baseUrl(firstNonBlank(modelProperties.getBaseUrl(), provider.getBaseUrl()))
                .endpointPath(
                        firstNonBlank(modelProperties.getEndpointPath(), provider.getEndpointPath()))
                .formatter(provider.getFormatter())
                .stream(modelProperties.isStream())
                .enableThinking(modelProperties.isEnableThinking())
                .additionalBodyParams(copyAdditionalBodyParams(provider.getGenerateKwargs()))
                .build();
    }

    /**
     * 返回当前目录快照中的供应商列表。
     *
     * @return 供应商列表
     */
    public List<TdAgentProviderDescriptor> listProviders() {
        return List.copyOf(currentSnapshot().providersById().values());
    }

    /**
     * 强制重新加载一次模型供应商目录。
     *
     * @return 重载后的目录快照时间
     */
    public Instant reloadCatalog() {
        return loadSnapshot(true).loadedAt();
    }

    private ProviderCatalogSnapshot currentSnapshot() {
        ProviderCatalogSnapshot snapshot = snapshotRef.get();
        if (snapshot == null) {
            return loadSnapshot(true);
        }
        if (!properties.getModel().isReloadOnChange()) {
            return snapshot;
        }
        Long lastModified = detectLastModified(snapshot.resource());
        if (!Objects.equals(lastModified, snapshot.lastModified())) {
            return loadSnapshot(true);
        }
        return snapshot;
    }

    private synchronized ProviderCatalogSnapshot loadSnapshot(boolean force) {
        ProviderCatalogSnapshot current = snapshotRef.get();
        if (!force && current != null) {
            return current;
        }
        Resource resource =
                resourceLoader.getResource(properties.getModel().getProviderConfigLocation());
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "内置模型目录文件不存在: " + properties.getModel().getProviderConfigLocation());
        }
        try (InputStream inputStream = resource.getInputStream()) {
            List<TdAgentProviderDescriptor> providers =
                    objectMapper.readValue(inputStream, PROVIDER_LIST_TYPE).stream()
                            .map(this::resolvePlaceholders)
                            .toList();
            if (providers.isEmpty()) {
                throw new IllegalStateException("内置模型目录中未配置任何供应商。");
            }
            Map<String, TdAgentProviderDescriptor> providersById = new LinkedHashMap<>();
            for (TdAgentProviderDescriptor provider : providers) {
                validateProvider(provider);
                String providerId = normalize(provider.getId(), null);
                if (providersById.putIfAbsent(providerId, provider) != null) {
                    throw new IllegalStateException("内置模型目录中存在重复的供应商 id: " + providerId);
                }
            }
            ProviderCatalogSnapshot snapshot =
                    new ProviderCatalogSnapshot(
                            providersById,
                            properties.getModel().getProviderConfigLocation(),
                            detectLastModified(resource),
                            Instant.now());
            snapshotRef.set(snapshot);
            return snapshot;
        } catch (IOException ex) {
            throw new IllegalStateException("加载内置模型目录失败。", ex);
        }
    }

    private TdAgentModelDescriptor resolveModel(
            TdAgentProviderDescriptor provider, String configuredModelId) {
        if (provider.getModels() == null || provider.getModels().isEmpty()) {
            throw new IllegalStateException("供应商未配置任何模型: " + provider.getId());
        }
        if (configuredModelId == null || configuredModelId.isBlank()) {
            return provider.getModels().get(0);
        }
        return provider.getModels().stream()
                .filter(model -> configuredModelId.equalsIgnoreCase(model.getId()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "供应商 "
                                                + provider.getId()
                                                + " 未配置模型: "
                                                + configuredModelId));
    }

    private TdAgentProviderDescriptor resolvePlaceholders(TdAgentProviderDescriptor provider) {
        TdAgentProviderDescriptor resolved = new TdAgentProviderDescriptor();
        resolved.setId(normalize(provider.getId(), null));
        resolved.setName(provider.getName());
        resolved.setProviderType(normalize(provider.getProviderType(), null));
        resolved.setBaseUrl(resolveEnvPlaceholder(provider.getBaseUrl()));
        resolved.setApiKey(resolveEnvPlaceholder(provider.getApiKey()));
        resolved.setEndpointPath(resolveEnvPlaceholder(provider.getEndpointPath()));
        resolved.setChatModel(provider.getChatModel());
        resolved.setFormatter(normalize(provider.getFormatter(), null));
        resolved.setApiKeyPrefix(provider.getApiKeyPrefix());
        resolved.setFreezeUrl(provider.isFreezeUrl());
        resolved.setCustom(provider.isCustom());
        resolved.setSupportModelDiscovery(provider.isSupportModelDiscovery());
        resolved.setSupportConnectionCheck(provider.isSupportConnectionCheck());
        resolved.setGenerateKwargs(copyAdditionalBodyParams(provider.getGenerateKwargs()));
        if (provider.getModels() != null) {
            resolved.setModels(
                    provider.getModels().stream().map(this::copyModelDescriptor).toList());
        }
        return resolved;
    }

    private TdAgentModelDescriptor copyModelDescriptor(TdAgentModelDescriptor model) {
        TdAgentModelDescriptor resolved = new TdAgentModelDescriptor();
        resolved.setId(normalize(model.getId(), null));
        resolved.setName(model.getName());
        resolved.setSupportsMultimodal(model.isSupportsMultimodal());
        resolved.setSupportsVideo(model.isSupportsVideo());
        resolved.setProbeSource(model.getProbeSource());
        return resolved;
    }

    private void validateProvider(TdAgentProviderDescriptor provider) {
        if (provider.getId() == null || provider.getId().isBlank()) {
            throw new IllegalStateException("内置模型目录存在缺少 id 的供应商配置。");
        }
        if (provider.getProviderType() == null || provider.getProviderType().isBlank()) {
            throw new IllegalStateException("供应商未配置 providerType: " + provider.getId());
        }
        TdAgentProviderType.fromValue(provider.getProviderType());
    }

    private Long detectLastModified(String resourceLocation) {
        return detectLastModified(resourceLoader.getResource(resourceLocation));
    }

    private Long detectLastModified(Resource resource) {
        try {
            if (resource.isFile()) {
                Path path = resource.getFile().toPath();
                return Files.getLastModifiedTime(path).toMillis();
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private Map<String, Object> copyAdditionalBodyParams(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>();
        source.forEach((key, value) -> copied.put(key, resolveObject(value)));
        return copied;
    }

    private Object resolveObject(Object value) {
        if (value instanceof String stringValue) {
            return resolveEnvPlaceholder(stringValue);
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> nested = new LinkedHashMap<>();
            mapValue.forEach((key, nestedValue) -> nested.put(String.valueOf(key), resolveObject(nestedValue)));
            return nested;
        }
        if (value instanceof List<?> listValue) {
            return listValue.stream().map(this::resolveObject).toList();
        }
        return value;
    }

    private String resolveEnvPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.startsWith("{{env:") && value.endsWith("}}")) {
            String envKey = value.substring(6, value.length() - 2).trim();
            return System.getenv(envKey);
        }
        return value;
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record ProviderCatalogSnapshot(
            Map<String, TdAgentProviderDescriptor> providersById,
            String resource,
            Long lastModified,
            Instant loadedAt) {}
}
