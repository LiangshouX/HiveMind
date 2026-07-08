package com.liangshou.agentic.agents.provider;

import com.liangshou.agentic.domain.shared.enums.TdAgentProviderType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商运行时参数映射 - 根据 providerId 推导 AgentScope 模型创建所需的运行时参数。
 *
 * <p>该映射表包含以下参数：</p>
 * <ul>
 *     <li><b>providerType</b>：供应商类型（DASHSCOPE / OPENAI），决定使用哪个 AgentScope 模型类</li>
 *     <li><b>formatter</b>：消息格式化器标识（null 表示使用默认格式化器）</li>
 *     <li><b>chatModelClassName</b>：AgentScope 模型实现类全限定名</li>
 *     <li><b>defaultGenerateKwargs</b>：默认的额外请求参数</li>
 * </ul>
 *
 * <p>对于未知的 providerId（用户自定义供应商），默认使用 OPENAI 类型。</p>
 *
 * @param providerType       供应商类型
 * @param formatter          消息格式化器标识，null 表示默认
 * @param chatModelClassName 模型实现类全限定名
 * @param defaultGenerateKwargs 默认额外请求参数
 * @author LiangshouX
 */
public record ProviderRuntimeParams(
        TdAgentProviderType providerType,
        String formatter,
        String chatModelClassName,
        Map<String, Object> defaultGenerateKwargs
) {

    /**
     * 内置供应商运行时参数映射表。
     */
    private static final Map<String, ProviderRuntimeParams> BUILTIN_PARAMS = new LinkedHashMap<>();

    static {
        BUILTIN_PARAMS.put("dashscope", new ProviderRuntimeParams(
                TdAgentProviderType.DASHSCOPE,
                null,
                "io.agentscope.core.model.DashScopeChatModel",
                Map.of("max_tokens", 8192)
        ));
        BUILTIN_PARAMS.put("deepseek", new ProviderRuntimeParams(
                TdAgentProviderType.OPENAI,
                "deepseek",
                "io.agentscope.core.model.OpenAIChatModel",
                Map.of("max_tokens", 65536)
        ));
        BUILTIN_PARAMS.put("openai", new ProviderRuntimeParams(
                TdAgentProviderType.OPENAI,
                null,
                "io.agentscope.core.model.OpenAIChatModel",
                Map.of()
        ));
    }

    /**
     * 默认参数（用于未知的自定义供应商）。
     */
    private static final ProviderRuntimeParams DEFAULT_PARAMS = new ProviderRuntimeParams(
            TdAgentProviderType.OPENAI,
            null,
            "io.agentscope.core.model.OpenAIChatModel",
            Map.of()
    );

    /**
     * 根据 providerId 解析运行时参数。
     *
     * <p>查找顺序：</p>
     * <ol>
     *     <li>精确匹配内置映射表</li>
     *     <li>providerId 包含 "deepseek" 时使用 deepseek 参数</li>
     *     <li>否则返回默认 OPENAI 参数</li>
     * </ol>
     *
     * @param providerId 供应商 ID
     * @return 运行时参数，永不为 null
     */
    public static ProviderRuntimeParams resolveOrDefault(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return DEFAULT_PARAMS;
        }
        ProviderRuntimeParams params = BUILTIN_PARAMS.get(providerId.toLowerCase().trim());
        if (params != null) {
            return params;
        }
        // 自定义供应商：根据 providerId 关键字推断
        if (providerId.toLowerCase().contains("deepseek")) {
            return BUILTIN_PARAMS.get("deepseek");
        }
        return DEFAULT_PARAMS;
    }

    /**
     * 返回所有内置供应商 ID 列表（用于调试和日志）。
     *
     * @return 内置供应商 ID 列表
     */
    public static List<String> builtinProviderIds() {
        return List.copyOf(BUILTIN_PARAMS.keySet());
    }
}
