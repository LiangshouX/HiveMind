package com.liangshou.agentic.infrastructure.provider.query;

import lombok.Data;

/**
 * 供应商配置查询结果 DTO - 供 Agent Engine 跨模块调用。
 *
 * <p>该 DTO 封装了从数据库查询到的供应商配置信息，
 * 包含 Agent 引擎创建模型实例所需的全部字段。</p>
 *
 * <p>注意：{@code encryptedApiKey} 为加密存储的 API Key，
 * 调用方需通过 {@code IProviderQueryService.getDecryptedApiKey()} 获取明文。</p>
 *
 * @author LiangshouX
 */
@Data
public class ProviderQueryResult {

    /** 记录主键 ID */
    private Long id;

    /** 用户 ID */
    private String userId;

    /** 供应商标识（如 dashscope、deepseek） */
    private String modelProviderId;

    /** 供应商显示名称 */
    private String providerName;

    /** 供应商类型：SYSTEM / CUSTOM / LOCAL */
    private String modelProviderType;

    /** 是否激活 */
    private boolean activated;

    /** 是否系统内置 */
    private boolean systemBuiltIn;

    /** API 访问地址 */
    private String baseUrl;

    /** 加密的 API Key */
    private String encryptedApiKey;

    /** 可用模型列表 JSON */
    private String modelsJson;

    /** 当前选中的模型 ID */
    private String selectedModelId;

    /** 当前选中的模型名称 */
    private String selectedModelName;
}
