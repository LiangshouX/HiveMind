package com.liangshou.tangdynasty.agentic.agents;

import com.liangshou.tangdynasty.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.tangdynasty.agentic.agents.provider.TdAgentResolvedModelConfig;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.springframework.stereotype.Component;

/**
 * Agent 模型工厂 - 根据配置创建 AgentScope 聊天模型实例。
 *
 * <p>该工厂负责：</p>
 * <ul>
 *     <li><b>模型创建</b>：根据 {@link TdAgentProviderRegistry} 解析的配置，创建对应的聊天模型实例</li>
 *     <li><b>供应商适配</b>：支持 DASHSCOPE 和 OPENAI 两种供应商类型，自动选择正确的模型实现和 Formatter</li>
 *     <li><b>配置应用</b>：将 API Key、Base URL、模型名称、流式选项等配置应用到模型实例</li>
 *     <li><b>额外参数</b>：支持通过 additionalBodyParams 传递供应商特定的生成参数</li>
 * </ul>
 *
 * <p>支持的模型类型：</p>
 * <ul>
 *     <li><b>DashScope</b>：使用 {@link io.agentscope.core.model.DashScopeChatModel} 和 DashScopeChatFormatter</li>
 *     <li><b>OpenAI Compatible</b>：使用 {@link io.agentscope.core.model.OpenAIChatModel}，支持标准 OpenAI 格式或 DeepSeek 格式</li>
 * </ul>
 *
 * <p>该工厂是单例 Spring Component，每次调用 {@link #create()} 都会根据最新配置创建新的模型实例。</p>
 *
 * @author LiangshouX
 */
@Component
public class TdAgentModelFactory {

    private final TdAgentProviderRegistry providerRegistry;

    /**
     * 创建模型工厂。
     *
     * @param providerRegistry 模型供应商目录注册表
     */
    public TdAgentModelFactory(TdAgentProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    /**
     * 根据当前生效的供应商配置创建聊天模型。
     *
     * @return 已配置的聊天模型
     */
    public Model create() {
        TdAgentResolvedModelConfig config = providerRegistry.resolveConfiguredModel();
        return switch (config.getProviderType()) {
            case DASHSCOPE -> createDashScopeModel(config);
            case OPENAI -> createOpenAiCompatibleModel(config);
        };
    }

    /**
     * 返回当前解析后的模型配置。
     *
     * @return 已解析的模型配置
     */
    public TdAgentResolvedModelConfig currentConfig() {
        return providerRegistry.resolveConfiguredModel();
    }

    private DashScopeChatModel createDashScopeModel(TdAgentResolvedModelConfig config) {
        return DashScopeChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelId())
                .stream(config.isStream())
                .enableThinking(config.isEnableThinking())
                .defaultOptions(buildGenerateOptions(config))
                .formatter(new DashScopeChatFormatter())
                .build();
    }

    private OpenAIChatModel createOpenAiCompatibleModel(TdAgentResolvedModelConfig config) {
        OpenAIChatModel.Builder builder =
                OpenAIChatModel.builder()
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl())
                        .modelName(config.getModelId())
                        .stream(config.isStream())
                        .generateOptions(buildGenerateOptions(config));
        if (config.getEndpointPath() != null && !config.getEndpointPath().isBlank()) {
            builder.endpointPath(config.getEndpointPath());
        }
        if ("deepseek".equalsIgnoreCase(config.getFormatter())) {
            builder.formatter(new DeepSeekFormatter());
        } else {
            builder.formatter(new OpenAIChatFormatter());
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions(TdAgentResolvedModelConfig config) {
        GenerateOptions.Builder builder =
                GenerateOptions.builder()
                        .modelName(config.getModelId())
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl())
                        .stream(config.isStream());
        if (config.getEndpointPath() != null && !config.getEndpointPath().isBlank()) {
            builder.endpointPath(config.getEndpointPath());
        }
        if (!config.getAdditionalBodyParams().isEmpty()) {
            builder.additionalBodyParams(config.getAdditionalBodyParams());
        }
        return builder.build();
    }
}
