package com.liangshou.tangdynasty.agentic.agents.provider.old.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ProviderInfo：供应商配置数据结构。
 * <p>
 * 用于承载静态配置与模型集合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderInfo {
    /**
     * 供应商标识（唯一 ID）
     */
    protected String id;

    /**
     * 供应商名称（人类可读）
     */
    protected String name;

    /**
     * API 基础 URL
     */
    protected String baseUrl;

    /**
     * API 密钥（敏感信息，外部使用时建议脱敏）
     */
    protected String apiKey;

    /**
     * ChatModel 类名（全限定名），用于反射加载或子类解析
     */
    protected String chatModel;

    /**
     * 内建/预置模型集合
     */
    protected List<ModelInfo> models = new ArrayList<>();

    /**
     * 用户追加的自定义模型集合
     */
    protected List<ModelInfo> extraModels = new ArrayList<>();

    /**
     * API Key 前缀规范（例如 "sk-"）
     */
    protected String apiKeyPrefix;

    /**
     * 是否为本地平台（如本地推理服务）
     */
    protected boolean isLocal;

    /**
     * 是否冻结 baseUrl（不可编辑）
     */
    protected boolean freezeUrl;

    /**
     * 是否要求提供 API Key
     */
    protected boolean requireApiKey = true;

    /**
     * 是否为用户自定义 Provider（非内建）
     */
    protected boolean isCustom;

    /**
     * 是否支持从平台 API 发现模型列表
     */
    protected boolean supportModelDiscovery;

    /**
     * 是否支持不带模型配置的连接性检查
     */
    protected boolean supportConnectionCheck = true;

    /**
     * 生成参数（传递给 ChatModel 的生成配置，如温度、max_tokens 等）
     */
    protected Map<String, Object> generateKwargs;
}
