package com.liangshou.service.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * Provider 数据传输对象
 * <p>用于创建和更新 Provider 信息</p>
 */
@Data
public class ProviderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Provider 标识（如 dashscope, openai） */
    private String modelProviderId;

    /** Provider 显示名称 */
    private String providerName;

    /** 供应商类型: SYSTEM/CUSTOM/LOCAL */
    private String modelProviderType;

    /** API 端点 URL */
    private String baseUrl;

    /** API Key（明文，保存时加密） */
    private String apiKey;

    /** 当前选中的模型 ID */
    private String modelId;

    /** 当前选中模型的显示名称 */
    private String modelName;

    /** 模型列表 JSON 数组 */
    private String modelsJson;

    /** 是否激活 */
    private Boolean activated;
}
