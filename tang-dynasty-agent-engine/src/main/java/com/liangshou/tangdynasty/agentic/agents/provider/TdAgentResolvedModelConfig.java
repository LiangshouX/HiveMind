package com.liangshou.tangdynasty.agentic.agents.provider;

import com.liangshou.tangdynasty.agentic.domain.shared.enums.TdAgentProviderType;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 已解析的模型配置 - 合并供应商目录与运行时配置后的最终模型配置。
 *
 * <p>该配置对象包含创建聊天模型所需的所有信息：</p>
 * <ul>
 *     <li><b>供应商标识</b>：providerId、providerName、providerType</li>
 *     <li><b>模型标识</b>：modelId、modelName</li>
 *     <li><b>连接信息</b>：apiKey、baseUrl、endpointPath</li>
 *     <li><b>格式化器</b>：formatter，决定消息格式（openai/deepseek）</li>
 *     <li><b>行为标志</b>：stream（是否流式响应）、enableThinking（是否启用思维链）</li>
 *     <li><b>额外参数</b>：additionalBodyParams，供应商特定的请求体参数</li>
 * </ul>
 *
 * <p>配置解析优先级：</p>
 * <ol>
 *     <li>应用配置 (application.yaml) 中的值优先</li>
 *     <li>其次使用供应商配置文件中的默认值</li>
 * </ol>
 *
 * <p>该配置由 {@link TdAgentProviderRegistry#resolveConfiguredModel()} 生成，
 * 并被 {@link com.liangshou.tangdynasty.agentic.agents.TdAgentModelFactory} 用于创建聊天模型实例。</p>
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class TdAgentResolvedModelConfig {

    private final String providerId;

    private final String providerName;

    private final TdAgentProviderType providerType;

    private final String modelId;

    private final String modelName;

    private final String apiKey;

    private final String baseUrl;

    private final String endpointPath;

    private final String formatter;

    private final boolean stream;

    private final boolean enableThinking;

    private final Map<String, Object> additionalBodyParams;

    /**
     * 返回可直接传递给 AgentScope GenerateOptions 的附加请求体参数。
     *
     * @return 不可变附加请求体参数
     */
    public Map<String, Object> getAdditionalBodyParams() {
        if (additionalBodyParams == null || additionalBodyParams.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(additionalBodyParams));
    }
}

