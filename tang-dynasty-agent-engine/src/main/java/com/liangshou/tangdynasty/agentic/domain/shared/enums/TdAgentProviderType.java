package com.liangshou.tangdynasty.agentic.domain.shared.enums;

import java.util.Locale;

/**
 * Agent 支持的模型供应商类型枚举。
 *
 * <p>目前支持的供应商类型：</p>
 * <ul>
 *     <li><b>DASHSCOPE</b> - 阿里云 DashScope（通义千问）平台</li>
 *     <li><b>OPENAI</b> - OpenAI 及兼容 OpenAI API 格式的平台（如 DeepSeek、Ollama 等）</li>
 * </ul>
 *
 * <p>通过 {@link #fromValue(String)} 方法可以从配置字符串解析出对应的枚举值，
 * 支持多种别名格式（如 "openai-compatible"、"openai_compatible" 都映射到 OPENAI）。</p>
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
