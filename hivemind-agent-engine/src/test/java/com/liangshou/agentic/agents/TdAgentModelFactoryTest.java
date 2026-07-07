package com.liangshou.agentic.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.OpenAIChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TdAgentModelFactory} 单元测试
 *
 * <p>本测试类验证模型工厂根据不同供应商配置正确创建聊天模型实例的能力，
 * 包括 DashScope（阿里云通义千问）和 OpenAI Compatible（兼容 OpenAI API 的模型）。</p>
 *
 * <p><strong>核心测试场景：</strong></p>
 * <ul>
 *     <li>DashScope 供应商创建 {@link io.agentscope.core.model.DashScopeChatModel}</li>
 *     <li>OpenAI Compatible 供应商创建 {@link io.agentscope.core.model.OpenAIChatModel}</li>
 *     <li>DeepSeek 等第三方服务通过 OpenAI 兼容模式接入</li>
 *     <li>模型配置的动态解析与应用（API Key、Base URL、模型ID等）</li>
 * </ul>
 *
 * <p><strong>工厂模式优势：</strong>通过统一的接口屏蔽不同供应商的实现细节，
 * 使得上层业务代码无需关心具体使用的是哪家厂商的模型，提升了系统的可扩展性和可维护性。</p>
 *
 * <p><strong>测试策略：</strong>使用临时目录创建模拟的供应商配置文件，
 * 避免依赖外部环境变量或真实的 API 密钥，确保测试的可重复性和隔离性。</p>
 *
 * @author LiangshouX
 * @see TdAgentModelFactory
 * @see com.liangshou.agentic.agents.provider.TdAgentProviderRegistry
 * @see io.agentscope.core.model.DashScopeChatModel
 * @see io.agentscope.core.model.OpenAIChatModel
 */
class TdAgentModelFactoryTest {

    @TempDir
    Path tempDir;

    /**
     * 测试 DashScope 模型的创建。
     *
     * <p>验证当配置指定使用 DashScope 供应商时，工厂能够正确创建
     * {@link io.agentscope.core.model.DashScopeChatModel} 实例。</p>
     *
     * <p><strong>配置示例：</strong></p>
     * <pre>{@code
     * providerId: dashscope
     * modelId: qwen-max
     * baseUrl: https://dashscope.aliyuncs.com/compatible-mode/v1
     * apiKey: dash-key
     * }</pre>
     *
     * <p><strong>验证点：</strong></p>
     * <ul>
     *     <li>返回的对象类型是 {@link io.agentscope.core.model.DashScopeChatModel}</li>
     *     <li>模型已正确配置 API Key 和模型名称</li>
     *     <li>可以使用 DashScope 特有的功能（如 enableThinking）</li>
     * </ul>
     *
     * <p><strong>业务意义：</strong>确保阿里云通义千问系列模型能够正常接入系统，
     * 支持中文场景下的高质量对话生成和复杂任务处理。</p>
     */
    @Test
    @DisplayName("应该能够创建 DashScope 模型实例")
    void shouldCreateDashScopeModel() throws Exception {
        TdAgentModelFactory factory = createFactory("dashscope", "qwen-max");

        assertInstanceOf(DashScopeChatModel.class, factory.create());
    }

    /**
     * 测试 DeepSeek 兼容模型的创建。
     *
     * <p>验证当配置指定使用 DeepSeek 供应商（基于 OpenAI 兼容协议）时，
     * 工厂能够正确创建 {@link io.agentscope.core.model.OpenAIChatModel} 实例。</p>
     *
     * <p><strong>配置示例：</strong></p>
     * <pre>{@code
     * providerId: deepseek
     * modelId: deepseek-chat
     * baseUrl: https://api.deepseek.com/v1
     * apiKey: deep-key
     * formatter: deepseek
     * }</pre>
     *
     * <p><strong>验证点：</strong></p>
     * <ul>
     *     <li>返回的对象类型是 {@link io.agentscope.core.model.OpenAIChatModel}</li>
     *     <li>使用了正确的 Base URL 指向 DeepSeek API</li>
     *     <li>应用了 DeepSeek 专用的 Formatter</li>
     * </ul>
     *
     * <p><strong>技术说明：</strong>DeepSeek 虽然是一家独立的 AI 公司，但其 API
     * 完全兼容 OpenAI 的接口规范，因此可以复用 OpenAI 的客户端实现，
     * 只需更换 Base URL 和 Formatter 即可。</p>
     *
     * <p><strong>扩展性体现：</strong>这种设计使得未来接入其他兼容 OpenAI API
     * 的服务商（如 Anthropic Claude、Google Gemini 等）变得非常简单，
     * 只需在配置文件中添加新的供应商条目即可，无需修改代码。</p>
     */
    @Test
    @DisplayName("应该能够创建 DeepSeek 兼容模型实例")
    void shouldCreateDeepSeekCompatibleModel() throws Exception {
        TdAgentModelFactory factory = createFactory("deepseek", "deepseek-chat");

        assertInstanceOf(OpenAIChatModel.class, factory.create());
    }

    private TdAgentModelFactory createFactory(String providerId, String modelId) throws Exception {
        Path catalog = tempDir.resolve("providers.json");
        Files.writeString(
                catalog,
                """
                        [
                          {
                            "id": "dashscope",
                            "name": "DashScope",
                            "providerType": "dashscope",
                            "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                            "apiKey": "dash-key",
                            "models": [
                              {
                                "id": "qwen-max",
                                "name": "Qwen Max"
                              }
                            ]
                          },
                          {
                            "id": "deepseek",
                            "name": "DeepSeek",
                            "providerType": "openai",
                            "baseUrl": "https://api.deepseek.com/v1",
                            "apiKey": "deep-key",
                            "formatter": "deepseek",
                            "models": [
                              {
                                "id": "deepseek-chat",
                                "name": "DeepSeek Chat"
                              }
                            ]
                          }
                        ]
                        """);
        TdAgentProperties properties = new TdAgentProperties();
        properties.getModel().setProviderConfigLocation(catalog.toUri().toString());
        properties.getModel().setProviderId(providerId);
        properties.getModel().setModelId(modelId);
        TdAgentProviderRegistry registry =
                new TdAgentProviderRegistry(
                        properties, new ObjectMapper(), new DefaultResourceLoader());
        registry.initialize();
        return new TdAgentModelFactory(registry);
    }
}