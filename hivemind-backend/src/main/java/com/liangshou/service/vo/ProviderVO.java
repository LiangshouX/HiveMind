package com.liangshou.service.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Provider 视图对象
 * <p>返回给前端的 Provider 信息，API Key 仅返回掩码</p>
 */
@Data
public class ProviderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Long id;

    /** Provider 标识（如 dashscope, openai） */
    private String modelProviderId;

    /** Provider 显示名称 */
    private String providerName;

    /** 供应商类型: SYSTEM/CUSTOM/LOCAL */
    private String modelProviderType;

    /** 是否激活 */
    private Boolean isProviderActivated;

    /** 是否系统内置 */
    private Boolean isSystemBuiltIn;

    /** API 端点 URL */
    private String baseUrl;

    /** API Key 掩码（如 sk-****xxxx），不返回明文 */
    private String apiKeyMask;

    /** 当前选中的模型 ID */
    private String modelId;

    /** 当前选中模型的显示名称 */
    private String modelName;

    /** 模型列表 JSON 数组 */
    private String modelsJson;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
