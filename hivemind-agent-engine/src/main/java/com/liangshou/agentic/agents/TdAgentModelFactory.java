package com.liangshou.agentic.agents;

import com.liangshou.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.agentic.agents.provider.TdAgentResolvedModelConfig;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TdAgentModelFactory.class);

    private final TdAgentProviderRegistry providerRegistry;

    /**
     * 构造模型工厂实例。
     *
     * @param providerRegistry 模型供应商注册表，负责解析和管理不同供应商（DashScope、OpenAI等）的配置信息
     */
    public TdAgentModelFactory(TdAgentProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    /**
     * 根据当前生效的供应商配置创建聊天模型实例。
     *
     * <p>该方法执行以下操作：</p>
     * <ol>
     *     <li>从 {@link TdAgentProviderRegistry} 解析当前配置的模型信息</li>
     *     <li>根据供应商类型（DASHSCOPE 或 OPENAI）选择对应的模型实现</li>
     *     <li>应用 API Key、Base URL、模型名称、流式选项等配置</li>
     *     <li>设置合适的 Formatter（DashScopeChatFormatter、OpenAIChatFormatter 或 DeepSeekFormatter）</li>
     *     <li>配置生成选项（GenerateOptions），包括额外参数</li>
     * </ol>
     *
     * <p><strong>支持的模型类型：</strong></p>
     * <ul>
     *     <li><b>DashScope</b>：阿里云通义千问系列模型，使用 {@link DashScopeChatModel}</li>
     *     <li><b>OpenAI Compatible</b>：兼容 OpenAI API 格式的模型（包括 OpenAI、DeepSeek 等），使用 {@link OpenAIChatModel}</li>
     * </ul>
     *
     * <p><strong>Formatter 选择逻辑：</strong></p>
     * <ul>
     *     <li>DashScope：始终使用 {@link DashScopeChatFormatter}</li>
     *     <li>OpenAI：如果配置中 {@code formatter="deepseek"}，使用 {@link DeepSeekFormatter}；否则使用 {@link OpenAIChatFormatter}</li>
     * </ul>
     *
     * <p><strong>注意：</strong>每次调用此方法都会创建新的模型实例，不会缓存或复用之前的实例。</p>
     *
     * @return 已配置的聊天模型实例，可直接用于 Agent 的消息生成
     * @throws IllegalStateException 如果配置中指定的供应商类型不受支持
     */
    public Model create() {
        TdAgentResolvedModelConfig config = providerRegistry.resolveConfiguredModel();
        log.info("[模型工厂] 开始创建模型实例 - provider: {}, modelId: {}, stream: {}", 
                config.getProviderType(), config.getModelId(), config.isStream());
        
        Model model = switch (config.getProviderType()) {
            case DASHSCOPE -> createDashScopeModel(config);
            case OPENAI -> createOpenAiCompatibleModel(config);
        };
        
        log.info("[模型工厂] 模型实例创建完成 - type: {}", model.getClass().getSimpleName());
        return model;
    }

    /**
     * 获取当前解析后的模型配置信息。
     *
     * <p>该方法从 {@link TdAgentProviderRegistry} 获取最新的模型配置，包括：</p>
     * <ul>
     *     <li>供应商类型（DASHSCOPE 或 OPENAI）</li>
     *     <li>API Key 和 Base URL</li>
     *     <li>模型ID/名称</li>
     *     <li>流式输出开关</li>
     *     <li>思考模式开关（仅 DashScope）</li>
     *     <li>端点路径（可选，用于自定义 API 路由）</li>
     *     <li>格式化器类型（仅 OpenAI Compatible）</li>
     *     <li>额外的请求体参数</li>
     * </ul>
     *
     * <p><strong>典型使用场景：</strong></p>
     * <ul>
     *     <li>调试时检查当前生效的模型配置</li>
     *     <li>在日志中记录使用的模型信息</li>
     *     <li>动态展示当前配置的模型详情给用户</li>
     * </ul>
     *
     * @return 已解析的模型配置对象，包含所有必要的连接和生成参数
     */
    public TdAgentResolvedModelConfig currentConfig() {
        return providerRegistry.resolveConfiguredModel();
    }

    /**
     * 创建 DashScope 聊天模型实例。
     *
     * <p>该方法专门用于创建阿里云通义千问系列的聊天模型，配置包括：</p>
     * <ul>
     *     <li>API Key 认证</li>
     *     <li>Base URL（可自定义，默认为阿里云官方地址）</li>
     *     <li>模型名称（如 qwen-max、qwen-plus 等）</li>
     *     <li>流式输出开关</li>
     *     <li>思考模式开关（enableThinking，用于启用模型的深度推理能力）</li>
     *     <li>默认生成选项（GenerateOptions）</li>
     *     <li>DashScopeChatFormatter 消息格式化器</li>
     * </ul>
     *
     * <p><strong>思考模式说明：</strong></p>
     * <p>当 {@code enableThinking=true} 时，模型会展示其推理过程，适用于需要复杂逻辑分析的场景。
     * 此特性是 DashScope 特有的功能，OpenAI Compatible 模型不支持。</p>
     *
     * @param config 已解析的模型配置，包含 DashScope 特定的参数
     * @return 配置完成的 DashScopeChatModel 实例
     */
    private DashScopeChatModel createDashScopeModel(TdAgentResolvedModelConfig config) {
        log.debug("[模型工厂-DashScope] 创建 DashScope 模型 - modelId: {}, stream: {}, enableThinking: {}", 
                config.getModelId(), config.isStream(), config.isEnableThinking());
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(config.getApiKey())
//                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelId())
                .stream(config.isStream())
                .enableThinking(config.isEnableThinking())
                .defaultOptions(buildGenerateOptions(config))
                .formatter(new DashScopeChatFormatter())
                .build();
        log.debug("[模型工厂-DashScope] DashScope 模型创建成功");
        return model;
    }

    /**
     * 创建 OpenAI 兼容格式的聊天模型实例。
     *
     * <p>该方法用于创建兼容 OpenAI API 格式的聊天模型，支持多种供应商：</p>
     * <ul>
     *     <li><b>OpenAI</b>：GPT-3.5、GPT-4 等系列</li>
     *     <li><b>DeepSeek</b>：DeepSeek-V2、DeepSeek-Coder 等</li>
     *     <li><b>其他兼容提供商</b>：任何遵循 OpenAI API 规范的服务</li>
     * </ul>
     *
     * <p>配置内容包括：</p>
     * <ul>
     *     <li>API Key 认证</li>
     *     <li>Base URL（可指向 OpenAI 官方或其他兼容服务）</li>
     *     <li>模型名称（如 gpt-4、deepseek-chat 等）</li>
     *     <li>流式输出开关</li>
     *     <li>端点路径（可选，用于自定义 API 路由，如 /v1/chat/completions）</li>
     *     <li>生成选项（GenerateOptions），包含额外参数</li>
     *     <li>消息格式化器（根据配置选择 DeepSeekFormatter 或 OpenAIChatFormatter）</li>
     * </ul>
     *
     * <p><strong>Formatter 选择逻辑：</strong></p>
     * <ul>
     *     <li>如果配置中 {@code formatter="deepseek"}（不区分大小写），使用 {@link DeepSeekFormatter}</li>
     *     <li>否则使用标准的 {@link OpenAIChatFormatter}</li>
     * </ul>
     *
     * <p><strong>端点路径说明：</strong></p>
     * <p>某些兼容服务可能需要自定义 API 端点路径（如 Azure OpenAI 的部署路径）。
     * 如果配置中提供了非空的 {@code endpointPath}，则会应用到模型构建器中。</p>
     *
     * @param config 已解析的模型配置，包含 OpenAI Compatible 特定的参数
     * @return 配置完成的 OpenAIChatModel 实例
     */
    private OpenAIChatModel createOpenAiCompatibleModel(TdAgentResolvedModelConfig config) {
        log.debug("[模型工厂-OpenAI] 创建 OpenAI 兼容模型 - modelId: {}, stream: {}, formatter: {}", 
                config.getModelId(), config.isStream(), config.getFormatter());
        
        OpenAIChatModel.Builder builder =
                OpenAIChatModel.builder()
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl())
                        .modelName(config.getModelId())
                        .stream(config.isStream())
                        .generateOptions(buildGenerateOptions(config));
        if (config.getEndpointPath() != null && !config.getEndpointPath().isBlank()) {
            builder.endpointPath(config.getEndpointPath());
            log.debug("[模型工厂-OpenAI] 使用自定义端点路径: {}", config.getEndpointPath());
        }
        if ("deepseek".equalsIgnoreCase(config.getFormatter())) {
            builder.formatter(new DeepSeekFormatter());
            log.debug("[模型工厂-OpenAI] 使用 DeepSeekFormatter");
        } else {
            builder.formatter(new OpenAIChatFormatter());
            log.debug("[模型工厂-OpenAI] 使用 OpenAIChatFormatter");
        }
        OpenAIChatModel model = builder.build();
        log.debug("[模型工厂-OpenAI] OpenAI 兼容模型创建成功");
        return model;
    }

    /**
     * 构建模型生成选项（GenerateOptions）。
     *
     * <p>该方法提取配置中的通用生成参数，构建 {@link GenerateOptions} 对象，包括：</p>
     * <ul>
     *     <li><b>modelName</b>：模型名称标识</li>
     *     <li><b>apiKey</b>：API 认证密钥</li>
     *     <li><b>baseUrl</b>：API 基础URL</li>
     *     <li><b>stream</b>：是否启用流式输出</li>
     *     <li><b>endpointPath</b>：自定义端点路径（如果配置中存在且非空）</li>
     *     <li><b>additionalBodyParams</b>：供应商特定的额外请求体参数（如果存在）</li>
     * </ul>
     *
     * <p><strong>额外参数说明：</strong></p>
     * <p>{@code additionalBodyParams} 允许传递供应商特定的生成参数，例如：</p>
     * <ul>
     *     <li>temperature：控制输出的随机性</li>
     *     <li>top_p：核采样参数</li>
     *     <li>max_tokens：最大生成长度</li>
     *     <li>presence_penalty：存在惩罚系数</li>
     *     <li>frequency_penalty：频率惩罚系数</li>
     * </ul>
     * <p>这些参数会直接添加到 HTTP 请求体中，由具体的模型供应商解析。</p>
     *
     * <p><strong>端点路径处理：</strong></p>
     * <p>如果配置中提供了非空且非空白的 {@code endpointPath}，则将其添加到生成选项中，
     * 用于覆盖默认的 API 路由（如从 /v1/chat/completions 改为自定义路径）。</p>
     *
     * @param config 已解析的模型配置，从中提取生成参数
     * @return 构建完成的 GenerateOptions 对象，包含所有必要的生成配置
     */
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
