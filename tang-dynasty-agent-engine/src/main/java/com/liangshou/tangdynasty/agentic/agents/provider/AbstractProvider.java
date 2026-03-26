package com.liangshou.tangdynasty.agentic.agents.provider;

import com.liangshou.tangdynasty.agentic.agents.provider.model.CheckResult;
import com.liangshou.tangdynasty.agentic.agents.provider.model.ModelInfo;
import com.liangshou.tangdynasty.agentic.agents.provider.model.ProbeResult;
import com.liangshou.tangdynasty.agentic.agents.provider.model.ProviderInfo;
import lombok.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Provider 抽象基类。
 * <p>
 * 该类在 {@link ProviderInfo} 的基础上，定义了与模型供应商交互的标准能力：
 * - 连接性检查（无模型配置）
 * - 拉取可用模型列表
 * - 检查特定模型连通性
 * - 动态新增/删除模型条目
 * - 更新供应商配置（遵循 freezeUrl / isCustom 等约束）
 * - 获取 ChatModel 类与实例（由具体子类实现）
 * - 多模态能力探针（默认空实现，子类可覆盖）
 * - 返回脱敏后的 ProviderInfo 快照
 * <p>
 * 异步语义通过 {@link CompletableFuture} 表达，以适配网络 IO/平台 API 交互。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractProvider extends ProviderInfo {
    /**
     * 检查当前配置下供应商的连通性（不依赖具体模型配置）。
     *
     * @param timeout 超时时间
     * @return 异步返回 CheckResult，ok=true 表示连通
     */
    public abstract CompletableFuture<CheckResult> checkConnection(Duration timeout);

    /**
     * 拉取供应商可用的模型列表。
     *
     * @param timeout 超时时间
     * @return 异步返回模型信息列表
     */
    public abstract CompletableFuture<List<ModelInfo>> fetchModels(Duration timeout);

    /**
     * 检查指定模型的连通性/可用性。
     *
     * @param modelId 模型标识
     * @param timeout 超时时间
     * @return 异步返回 CheckResult，ok=true 表示模型可用
     */
    public abstract CompletableFuture<CheckResult> checkModelConnection(String modelId, Duration timeout);

    /**
     * 新增模型到指定集合。
     * <p>
     * target 支持：
     * - "models"：内建/预置模型集合
     * - "extra_models"：用户追加的自定义模型集合
     *
     * @param modelInfo 模型信息
     * @param target    目标集合（"models" 或 "extra_models"）
     * @param timeout   超时时间（语义保留）
     * @return 异步返回添加是否成功及消息
     */
    public CompletableFuture<CheckResult> addModel(ModelInfo modelInfo, String target, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            for (ModelInfo m : getAllModels()) {
                if (Objects.equals(m.getId(), modelInfo.getId())) {
                    return CheckResult.error("Model '" + modelInfo.getId() + "' already exists");
                }
            }
            if ("extra_models".equals(target)) {
                this.extraModels.add(modelInfo);
            } else if ("models".equals(target)) {
                this.models.add(modelInfo);
            } else {
                return CheckResult.error("Invalid target '" + target + "' for adding model");
            }
            return CheckResult.ok();
        });
    }

    /**
     * 从 extra_models 集合中删除指定模型。
     *
     * @param modelId 模型标识
     * @param timeout 超时时间（语义保留）
     * @return 异步返回删除是否成功及消息
     */
    public CompletableFuture<CheckResult> deleteModel(String modelId, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> {
            this.extraModels.removeIf(m -> Objects.equals(m.getId(), modelId));
            return CheckResult.ok();
        });
    }

    /**
     * 更新供应商配置。
     * <p>
     * - 若 freezeUrl=true，则忽略 base_url 更新
     * - 若 isCustom=true，允许自定义 chat_model
     * - generate_kwargs 需为 Map 类型
     *
     * @param config 配置字典（支持 name、base_url、api_key、chat_model、api_key_prefix、generate_kwargs）
     */
    public void updateConfig(Map<String, Object> config) {
        if (config == null) return;
        if (config.containsKey("name") && config.get("name") != null) {
            this.setName(String.valueOf(config.get("name")));
        }
        if (!this.isFreezeUrl() && config.containsKey("base_url") && config.get("base_url") != null) {
            this.setBaseUrl(String.valueOf(config.get("base_url")));
        }
        if (config.containsKey("api_key") && config.get("api_key") != null) {
            this.setApiKey(String.valueOf(config.get("api_key")));
        }
        if (this.isCustom() && config.containsKey("chat_model") && config.get("chat_model") != null) {
            this.setChatModel(String.valueOf(config.get("chat_model")));
        }
        if (config.containsKey("api_key_prefix") && config.get("api_key_prefix") != null) {
            this.setApiKeyPrefix(String.valueOf(config.get("api_key_prefix")));
        }
        if (config.containsKey("generate_kwargs") && config.get("generate_kwargs") instanceof Map<?, ?> m) {
            Map<String, Object> newKwargs = new HashMap<>();
            m.forEach((k, v) -> newKwargs.put(String.valueOf(k), v));
            this.setGenerateKwargs(newKwargs);
        }
    }

    /**
     * 根据 chatModel 的全限定类名加载模型类。
     *
     * @return ChatModel 类对象
     * @throws IllegalStateException    当 chatModel 为空
     * @throws IllegalArgumentException 当指定类名未找到
     */
    public Class<?> getChatModelClass() {
        if (this.getChatModel() == null || this.getChatModel().isEmpty()) {
            throw new IllegalStateException("Chat model class name is empty for provider '" + this.getName() + "'");
        }
        try {
            return Class.forName(this.getChatModel());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Chat model class '" + this.getChatModel() + "' not found for provider '" + this.getName() + "'", e);
        }
    }

    /**
     * 判断是否存在指定模型。
     *
     * @param modelId 模型标识
     * @return 存在返回 true，否则返回 false
     */
    public boolean hasModel(String modelId) {
        for (ModelInfo m : getAllModels()) {
            if (Objects.equals(m.getId(), modelId)) return true;
        }
        return false;
    }

    /**
     * 获取指定模型对应的 ChatModel 实例。
     * <p>
     * 具体实现由子类负责（例如适配 AgentScope/OpenAI/Ollama 等）。
     *
     * @param modelId 模型标识
     * @return ChatModel 实例对象
     */
    public abstract Object getChatModelInstance(String modelId);

    /**
     * 探针：检测模型是否支持多模态（图片/视频等）。
     * <p>
     * 默认返回全 false 的 {@link ProbeResult}；有 API 能力的子类应覆盖实现。
     *
     * @param modelId 模型标识
     * @param timeout 超时时间（语义保留）
     * @return 异步返回探针结果
     */
    public CompletableFuture<ProbeResult> probeModelMultimodal(String modelId, Duration timeout) {
        return CompletableFuture.completedFuture(new ProbeResult(false, false, false, null));
    }

    /**
     * 获取供应商信息快照。
     * <p>
     * 当 mockSecret=true 且 apiKey 非空时，将使用 apiKeyPrefix + "******" 的形式进行脱敏。
     *
     * @param mockSecret 是否脱敏返回 apiKey
     * @return 异步返回 ProviderInfo 快照
     */
    public CompletableFuture<ProviderInfo> getInfo(boolean mockSecret) {
        return CompletableFuture.supplyAsync(() -> {
            String key = this.getApiKey();
            if (mockSecret && key != null && !key.isEmpty()) {
                key = this.getApiKeyPrefix() + "******";
            }
            ProviderInfo info = new ProviderInfo();
            info.setId(this.getId());
            info.setName(this.getName());
            info.setBaseUrl(this.getBaseUrl());
            info.setApiKey(key);
            info.setChatModel(this.getChatModel());
            info.setModels(new ArrayList<>(this.getModels()));
            info.setExtraModels(new ArrayList<>(this.getExtraModels()));
            info.setApiKeyPrefix(this.getApiKeyPrefix());
            info.setLocal(this.isLocal());
            info.setCustom(this.isCustom());
            info.setSupportModelDiscovery(this.isSupportModelDiscovery());
            info.setSupportConnectionCheck(this.isSupportConnectionCheck() && !this.isCustom());
            info.setFreezeUrl(this.isFreezeUrl());
            info.setRequireApiKey(this.isRequireApiKey());
            info.setGenerateKwargs(this.getGenerateKwargs() == null ? Map.of() : this.getGenerateKwargs());
            return info;
        });
    }

    private List<ModelInfo> getAllModels() {
        List<ModelInfo> list = new ArrayList<>(this.getModels().size() + this.getExtraModels().size());
        list.addAll(this.getModels());
        list.addAll(this.getExtraModels());
        return list;
    }
}
