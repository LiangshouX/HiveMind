package com.liangshou.tangdynasty.agentic.agents.provider;

import com.liangshou.tangdynasty.agentic.common.enums.TdAgentProviderType;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应商目录与运行时覆盖配置合并后的模型配置。
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

