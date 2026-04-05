package com.liangshou.tangdynasty.agentic.common.enums;

import java.util.Locale;

/**
 * TDAgent 支持的模型供应商类型。
 *
 * @author LiangshouX
 */
public enum TdAgentProviderType {
    DASHSCOPE,
    OPENAI;

    /**
     * 根据配置标识解析供应商类型。
     *
     * @param value 配置中的供应商类型标识
     * @return 解析后的供应商类型
     */
    public static TdAgentProviderType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("模型供应商类型不能为空。");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "dashscope" -> DASHSCOPE;
            case "openai", "openai-compatible", "openai_compatible" -> OPENAI;
            default -> throw new IllegalArgumentException("不支持的模型供应商类型: " + value);
        };
    }
}
