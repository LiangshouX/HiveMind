package com.liangshou.tangdynasty.agentic.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.liangshou.tangdynasty.agentic.agents.provider.old.AbstractProvider;
import com.liangshou.tangdynasty.agentic.agents.provider.old.impl.DashScopeProvider;
import com.liangshou.tangdynasty.agentic.agents.provider.old.impl.OpenAIProvider;
import com.liangshou.tangdynasty.agentic.agents.provider.old.model.ModelInfo;
import com.liangshou.tangdynasty.agentic.agents.provider.old.model.ProviderInfo;
import com.liangshou.tangdynasty.agentic.common.exceptions.BizException;
import com.liangshou.tangdynasty.agentic.service.IProviderManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.liangshou.tangdynasty.agentic.common.exceptions.ErrorCodeEnum.LOAD_PROVIDER_ERROR;

/**
 * 供应商管理服务实现 - 从 JSON 配置文件加载和管理模型供应商。
 *
 * <p>该实现提供以下功能：</p>
 * <ul>
 *     <li><b>配置加载</b>：从 classpath:provider/builtin_provider.json 和 custom_provider.json 加载供应商配置</li>
 *     <li><b>缓存管理</b>：使用 ConcurrentHashMap 缓存已加载的供应商，避免重复解析</li>
 *     <li><b>动态解析</b>：根据 chatModel 字段自动选择 DashScopeProvider 或 OpenAIProvider 实现</li>
 *     <li><b>模型列表解析</b>：解析供应商的 models 和 extraModels 列表，构建完整的模型信息</li>
 * </ul>
 *
 * <p>工作流程：</p>
 * <ol>
 *     <li>首次调用 {@link #getProvider(String)} 时触发 {@link #loadAllProviders()}</li>
 *     <li>读取 JSON 文件并解析为 JSONArray</li>
 *     <li>遍历数组，根据配置创建对应的 AbstractProvider 实例</li>
 *     <li>将供应商存入缓存并返回</li>
 * </ol>
 *
 * <p>注意：该类位于旧版实现中，建议优先使用新的 {@link com.liangshou.tangdynasty.agentic.agents.provider.TdAgentProviderRegistry}。</p>
 *
 * @author LiangshouX
 */
@SuppressWarnings("unused")
public class ProviderManagerServiceImpl implements IProviderManagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderManagerServiceImpl.class);

    private static final String PROVIDER_PATH_PREFIX = "provider/";

    private static final String BUILTIN_PROVIDER_FILE = "builtin_provider.json";

    private static final String CUSTOM_PROVIDER_FILE = "custom_provider.json";

    /**
     * 供应商配置缓存，避免重复加载。
     */
    private final Map<String, AbstractProvider> providerCache = new ConcurrentHashMap<>();

    @Override
    public ProviderInfo getProviderInfo(String providerId) {
        AbstractProvider provider = getProvider(providerId);
        if (provider == null) {
            return null;
        }
        // 使用 getInfo 方法获取快照，并脱敏 API Key
        return provider.getInfo(true).join();
    }

    @Override
    public AbstractProvider getProvider(String providerId) {
        // 先从缓存中获取
        AbstractProvider cached = providerCache.get(providerId);
        if (cached != null) {
            return cached;
        }

        // 加载所有供应商配置
        loadAllProviders();

        // 再次从缓存中获取
        return providerCache.get(providerId);
    }

    /**
     * 加载所有内置和自定义的供应商配置。
     */
    private synchronized void loadAllProviders() {
        // 如果已经加载过，直接返回
        if (!providerCache.isEmpty()) {
            return;
        }

        try {
            // 加载内置供应商配置
            loadProvidersFromFile(BUILTIN_PROVIDER_FILE);

            // 加载自定义供应商配置
            loadProvidersFromFile(CUSTOM_PROVIDER_FILE);

            LOGGER.info("Loaded {} providers", providerCache.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load providers");
            throw new BizException(LOAD_PROVIDER_ERROR, e);
        }
    }

    /**
     * 从指定的 JSON 文件加载供应商配置。
     *
     * @param fileName JSON 文件名
     */
    private void loadProvidersFromFile(String fileName) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource(PROVIDER_PATH_PREFIX + fileName);

            if (!resource.exists()) {
                LOGGER.warn("Provider file not found: {}", fileName);
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);

                // 处理空文件或空数组
                if (content.trim().isEmpty()) {
                    LOGGER.debug("Provider file is empty: {}", fileName);
                    return;
                }

                JSONArray jsonArray = JSON.parseArray(content);
                if (jsonArray == null || jsonArray.isEmpty()) {
                    LOGGER.debug("No providers found in: {}", fileName);
                    return;
                }

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject != null) {
                        AbstractProvider provider = parseProvider(jsonObject);
                        if (provider.getId() != null) {
                            providerCache.put(provider.getId(), provider);
                            LOGGER.debug("Loaded provider: {} ({})", provider.getName(), provider.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load providers from file: {}", fileName, e);
        }
    }

    /**
     * 从 JSON 对象解析出 Provider 实例。
     *
     * @param jsonObject JSON 配置对象
     * @return 解析后的 Provider 实例
     */
    private AbstractProvider parseProvider(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String chatModel = jsonObject.getString("chatModel");

        AbstractProvider provider;

        // 根据 chatModel 或 id 判断使用哪个 Provider 实现类
        if (chatModel != null && chatModel.contains("DashScope")) {
            provider = new DashScopeProvider(false);
        } else if (chatModel != null && chatModel.contains("OpenAI")) {
            provider = new OpenAIProvider(false);
        } else {
            // 默认使用 OpenAIProvider 作为通用实现
            provider = new OpenAIProvider(false);
        }

        // 设置基本信息
        provider.setId(id);
        provider.setName(jsonObject.getString("name"));
        provider.setBaseUrl(jsonObject.getString("baseUrl"));
        provider.setApiKey(jsonObject.getString("apiKey"));
        provider.setChatModel(chatModel);
        provider.setApiKeyPrefix(jsonObject.getString("apiKeyPrefix"));
        provider.setFreezeUrl(jsonObject.getBooleanValue("freezeUrl"));
        provider.setCustom(jsonObject.getBooleanValue("isCustom"));
        provider.setSupportModelDiscovery(jsonObject.getBooleanValue("supportModelDiscovery"));
        provider.setSupportConnectionCheck(jsonObject.getBooleanValue("supportConnectionCheck"));

        // 处理本地平台标识
        if (jsonObject.containsKey("isLocal")) {
            provider.setLocal(jsonObject.getBooleanValue("isLocal"));
        }

        // 处理 API Key 要求
        if (jsonObject.containsKey("requireApiKey")) {
            provider.setRequireApiKey(jsonObject.getBooleanValue("requireApiKey"));
        }

        // 解析模型列表
        List<ModelInfo> models = parseModels(jsonObject.getJSONArray("models"));
        provider.setModels(models);

        // 解析额外模型列表
        List<ModelInfo> extraModels = parseModels(jsonObject.getJSONArray("extraModels"));
        provider.setExtraModels(extraModels);

        // 解析生成参数
        JSONObject generateKwargs = jsonObject.getJSONObject("generateKwargs");
        if (generateKwargs != null && !generateKwargs.isEmpty()) {
            provider.setGenerateKwargs(generateKwargs.toJavaObject(Map.class));
        }

        return provider;
    }

    /**
     * 解析模型列表。
     *
     * @param jsonArray 模型 JSON 数组
     * @return 模型信息列表
     */
    private List<ModelInfo> parseModels(JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.isEmpty()) {
            return new ArrayList<>();
        }

        List<ModelInfo> models = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject modelJson = jsonArray.getJSONObject(i);
            if (modelJson != null) {
                ModelInfo model = ModelInfo.builder()
                        .id(modelJson.getString("id"))
                        .name(modelJson.getString("name"))
                        .supportsMultimodal(modelJson.getBoolean("supportsMultimodal"))
                        .supportsVideo(modelJson.getBoolean("supportsVideo"))
                        .probeSource(modelJson.getString("probeSource"))
                        .build();
                models.add(model);
            }
        }
        return models;
    }
}
