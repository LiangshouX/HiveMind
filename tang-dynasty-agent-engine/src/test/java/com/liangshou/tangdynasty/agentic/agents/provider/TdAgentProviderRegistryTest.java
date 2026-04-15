package com.liangshou.tangdynasty.agentic.agents.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.common.enums.TdAgentProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link TdAgentProviderRegistry} 单元测试
 *
 * @author LiangshouX
 */
@DisplayName("TdAgentProviderRegistry 单元测试")
class TdAgentProviderRegistryTest {
    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private DefaultResourceLoader resourceLoader;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resourceLoader = new DefaultResourceLoader();
    }

    @Nested
    @DisplayName("初始化测试")
    class InitializeTests {

        @Test
        @DisplayName("应该成功初始化并加载内置供应商目录")
        void shouldInitializeSuccessfullyWithValidCatalog() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);

            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);

            assertDoesNotThrow(registry::initialize);
        }

        @Test
        @DisplayName("当目录文件不存在时应抛出异常")
        void shouldThrowExceptionWhenCatalogFileNotExist() {
            TdAgentProperties properties = new TdAgentProperties();
            properties.getModel().setProviderConfigLocation("classpath:non-existent.json");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);

            assertThrows(IllegalStateException.class, registry::initialize);
        }

        @Test
        @DisplayName("当目录为空列表时应抛出异常")
        void shouldThrowExceptionWhenCatalogIsEmpty() throws Exception {
            Path catalog = createProviderCatalog("[]");
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);

            assertThrows(IllegalStateException.class, registry::initialize);
        }

        @Test
        @DisplayName("当供应商缺少id时应抛出异常")
        void shouldThrowExceptionWhenProviderMissingId() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "name": "No ID Provider",
                        "providerType": "openai",
                        "baseUrl": "https://api.openai.com/v1",
                        "models": []
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, null, null);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);

            assertThrows(IllegalStateException.class, () -> registry.initialize());
        }

        @Test
        @DisplayName("当供应商缺少providerType时应抛出异常")
        void shouldThrowExceptionWhenProviderMissingProviderType() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "test-provider",
                        "name": "Test Provider",
                        "baseUrl": "https://api.test.com/v1",
                        "models": []
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "test-provider", null);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);

            assertThrows(IllegalStateException.class, () -> registry.initialize());
        }

        @Test
        @DisplayName("当存在重复的供应商id时应抛出异常")
        void shouldThrowExceptionWhenDuplicateProviderIds() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "duplicate",
                        "name": "First",
                        "providerType": "openai",
                        "baseUrl": "https://api1.com/v1",
                        "models": []
                      },
                      {
                        "id": "duplicate",
                        "name": "Second",
                        "providerType": "openai",
                        "baseUrl": "https://api2.com/v1",
                        "models": []
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "duplicate", null);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);

            assertThrows(IllegalStateException.class, () -> registry.initialize());
        }

        @Test
        @DisplayName("当providerType无效时应抛出异常")
        void shouldThrowExceptionWhenInvalidProviderType() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "invalid-type",
                        "name": "Invalid Type",
                        "providerType": "invalid_provider_type",
                        "baseUrl": "https://api.invalid.com/v1",
                        "models": []
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "invalid-type", null);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);

            assertThrows(IllegalArgumentException.class, () -> registry.initialize());
        }
    }

    @Nested
    @DisplayName("解析配置模型测试")
    class ResolveConfiguredModelTests {

        @Test
        @DisplayName("应该从内置目录解析 DashScope 模型配置")
        void shouldResolveDashScopeModelFromBuiltinCatalog() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals(TdAgentProviderType.DASHSCOPE, config.getProviderType());
            assertEquals("qwen-max", config.getModelId());
            assertEquals("dash-key", config.getApiKey());
            assertEquals("dashscope", config.getProviderId());
            assertEquals("DashScope", config.getProviderName());
            assertEquals("Qwen Max", config.getModelName());
        }

        @Test
        @DisplayName("当配置的供应商不存在时应抛出异常")
        void shouldThrowExceptionWhenProviderNotFound() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "nonexistent", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            assertThrows(IllegalStateException.class, () -> registry.resolveConfiguredModel());
        }

        @Test
        @DisplayName("当配置的模型不存在时应抛出异常")
        void shouldThrowExceptionWhenModelNotFound() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "nonexistent-model");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            assertThrows(IllegalStateException.class, () -> registry.resolveConfiguredModel());
        }

        @Test
        @DisplayName("当API Key未配置时应抛出异常")
        void shouldThrowExceptionWhenApiKeyNotConfigured() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "dashscope",
                        "name": "DashScope",
                        "providerType": "dashscope",
                        "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "apiKey": "",
                        "models": [
                          {
                            "id": "qwen-max",
                            "name": "Qwen Max"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            properties.getModel().setApiKey(null);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            assertThrows(IllegalStateException.class, () -> registry.resolveConfiguredModel());
        }

        @Test
        @DisplayName("当供应商没有配置任何模型时应抛出异常")
        void shouldThrowExceptionWhenProviderHasNoModels() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "empty-provider",
                        "name": "Empty Provider",
                        "providerType": "openai",
                        "baseUrl": "https://api.empty.com/v1",
                        "apiKey": "test-key",
                        "models": []
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "empty-provider", null);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            assertThrows(IllegalStateException.class, () -> registry.resolveConfiguredModel());
        }

        @Test
        @DisplayName("当未指定模型ID时应使用第一个模型")
        void shouldUseFirstModelWhenModelIdNotSpecified() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "openai",
                        "name": "OpenAI",
                        "providerType": "openai",
                        "baseUrl": "https://api.openai.com/v1",
                        "apiKey": "openai-key",
                        "models": [
                          {
                            "id": "gpt-4",
                            "name": "GPT-4"
                          },
                          {
                            "id": "gpt-3.5-turbo",
                            "name": "GPT-3.5 Turbo"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "openai", null);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("gpt-4", config.getModelId());
            assertEquals("GPT-4", config.getModelName());
        }

        @Test
        @DisplayName("应该使用配置中的API Key覆盖供应商的API Key")
        void shouldOverrideApiKeyFromConfiguration() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "dashscope",
                        "name": "DashScope",
                        "providerType": "dashscope",
                        "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "apiKey": "catalog-key",
                        "models": [
                          {
                            "id": "qwen-max",
                            "name": "Qwen Max"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            properties.getModel().setApiKey("config-key");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("config-key", config.getApiKey());
        }

        @Test
        @DisplayName("应该使用配置中的Base URL覆盖供应商的Base URL")
        void shouldOverrideBaseUrlFromConfiguration() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "openai",
                        "name": "OpenAI",
                        "providerType": "openai",
                        "baseUrl": "https://api.openai.com/v1",
                        "apiKey": "openai-key",
                        "models": [
                          {
                            "id": "gpt-4",
                            "name": "GPT-4"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "openai", "gpt-4");
            properties.getModel().setBaseUrl("https://custom-proxy.com/v1");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("https://custom-proxy.com/v1", config.getBaseUrl());
        }

        @Test
        @DisplayName("应该正确解析formatter字段")
        void shouldResolveFormatterField() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
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
            TdAgentProperties properties = createProperties(catalog, "deepseek", "deepseek-chat");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("deepseek", config.getFormatter());
        }

        @Test
        @DisplayName("应该正确解析generateKwargs作为additionalBodyParams")
        void shouldResolveGenerateKwargsAsAdditionalBodyParams() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "custom-provider",
                        "name": "Custom Provider",
                        "providerType": "openai",
                        "baseUrl": "https://api.custom.com/v1",
                        "apiKey": "custom-key",
                        "generateKwargs": {
                          "temperature": 0.7,
                          "top_p": 0.9,
                          "max_tokens": 2000
                        },
                        "models": [
                          {
                            "id": "custom-model",
                            "name": "Custom Model"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "custom-provider", "custom-model");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            Map<String, Object> additionalParams = config.getAdditionalBodyParams();
            assertEquals(0.7, additionalParams.get("temperature"));
            assertEquals(0.9, additionalParams.get("top_p"));
            assertEquals(2000, additionalParams.get("max_tokens"));
        }

        @Test
        @DisplayName("应该正确解析stream和enableThinking配置")
        void shouldResolveStreamAndEnableThinkingFlags() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            properties.getModel().setStream(true);
            properties.getModel().setEnableThinking(true);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertTrue(config.isStream());
            assertTrue(config.isEnableThinking());
        }

        @Test
        @DisplayName("应该支持modelName作为modelId的备选")
        void shouldUseModelNameAsFallbackForModelId() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "dashscope",
                        "name": "DashScope",
                        "providerType": "dashscope",
                        "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "apiKey": "dash-key",
                        "models": [
                          {
                            "id": "qwen-plus",
                            "name": "Qwen Plus"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = new TdAgentProperties();
            properties.getModel().setProviderConfigLocation(catalog.toUri().toString());
            properties.getModel().setProviderId("dashscope");
            properties.getModel().setModelId(null);
            properties.getModel().setModelName("qwen-plus");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("qwen-plus", config.getModelId());
        }

        @Test
        @DisplayName("应该支持endpointPath配置")
        void shouldResolveEndpointPath() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "custom",
                        "name": "Custom",
                        "providerType": "openai",
                        "baseUrl": "https://api.custom.com",
                        "apiKey": "custom-key",
                        "endpointPath": "/chat/completions",
                        "models": [
                          {
                            "id": "model-1",
                            "name": "Model 1"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "custom", "model-1");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("/chat/completions", config.getEndpointPath());
        }
    }

    @Nested
    @DisplayName("列出供应商测试")
    class ListProvidersTests {

        @Test
        @DisplayName("应该返回所有已加载的供应商")
        void shouldReturnAllLoadedProviders() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "dashscope",
                        "name": "DashScope",
                        "providerType": "dashscope",
                        "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "apiKey": "dash-key",
                        "models": [{"id": "qwen-max", "name": "Qwen Max"}]
                      },
                      {
                        "id": "openai",
                        "name": "OpenAI",
                        "providerType": "openai",
                        "baseUrl": "https://api.openai.com/v1",
                        "apiKey": "openai-key",
                        "models": [{"id": "gpt-4", "name": "GPT-4"}]
                      },
                      {
                        "id": "deepseek",
                        "name": "DeepSeek",
                        "providerType": "openai",
                        "baseUrl": "https://api.deepseek.com/v1",
                        "apiKey": "deep-key",
                        "models": [{"id": "deepseek-chat", "name": "DeepSeek Chat"}]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            List<TdAgentProviderDescriptor> providers = registry.listProviders();

            assertEquals(3, providers.size());
            assertTrue(providers.stream().anyMatch(p -> "dashscope".equals(p.getId())));
            assertTrue(providers.stream().anyMatch(p -> "openai".equals(p.getId())));
            assertTrue(providers.stream().anyMatch(p -> "deepseek".equals(p.getId())));
        }

        @Test
        @DisplayName("应该返回不可修改的供应商列表")
        void shouldReturnUnmodifiableProviderList() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "dashscope",
                        "name": "DashScope",
                        "providerType": "dashscope",
                        "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "apiKey": "dash-key",
                        "models": [{"id": "qwen-max", "name": "Qwen Max"}]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            List<TdAgentProviderDescriptor> providers = registry.listProviders();

            assertThrows(UnsupportedOperationException.class, () -> providers.add(null));
        }
    }

    @Nested
    @DisplayName("重新加载目录测试")
    class ReloadCatalogTests {

        @Test
        @DisplayName("当文件变化且reloadOnChange启用时应该自动重载")
        void shouldReloadCatalogWhenFileChanges() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "deepseek", "deepseek-chat");
            properties.getModel().setReloadOnChange(true);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            assertThrows(IllegalStateException.class, () -> registry.resolveConfiguredModel());

            Thread.sleep(20L);
            Files.writeString(
                    catalog,
                    """
                            [
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

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals(TdAgentProviderType.OPENAI, config.getProviderType());
            assertEquals("deep-key", config.getApiKey());
            assertEquals("deepseek", config.getFormatter());
        }

        @Test
        @DisplayName("当reloadOnChange禁用时不应该自动重载")
        void shouldNotAutoReloadWhenReloadOnChangeDisabled() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            properties.getModel().setReloadOnChange(false);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config1 = registry.resolveConfiguredModel();
            assertEquals("dash-key", config1.getApiKey());

            Thread.sleep(20L);
            Files.writeString(
                    catalog,
                    """
                            [
                              {
                                "id": "dashscope",
                                "name": "DashScope Updated",
                                "providerType": "dashscope",
                                "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                                "apiKey": "new-dash-key",
                                "models": [
                                  {
                                    "id": "qwen-max",
                                    "name": "Qwen Max"
                                  }
                                ]
                              }
                            ]
                            """);

            TdAgentResolvedModelConfig config2 = registry.resolveConfiguredModel();

            assertEquals("dash-key", config2.getApiKey());
        }

        @Test
        @DisplayName("应该支持手动强制重新加载目录")
        void shouldSupportManualForceReload() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            properties.getModel().setReloadOnChange(false);
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            Thread.sleep(20L);
            Files.writeString(
                    catalog,
                    """
                            [
                              {
                                "id": "dashscope",
                                "name": "DashScope Updated",
                                "providerType": "dashscope",
                                "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                                "apiKey": "new-dash-key",
                                "models": [
                                  {
                                    "id": "qwen-max",
                                    "name": "Qwen Max"
                                  }
                                ]
                              }
                            ]
                            """);

            registry.reloadCatalog();
            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("new-dash-key", config.getApiKey());
            assertEquals("DashScope Updated", config.getProviderName());
        }
    }

    @Nested
    @DisplayName("环境变量占位符解析测试")
    class EnvironmentPlaceholderTests {

        @Test
        @DisplayName("应该解析baseUrl中的环境变量占位符")
        void shouldResolveEnvPlaceholderInBaseUrl() throws Exception {
            String originalValue = System.getenv("TEST_BASE_URL");
            try {
                // 注意：在实际测试中，设置环境变量需要使用特殊工具
                // 这里仅测试占位符格式的处理逻辑
                Path catalog = createProviderCatalog("""
                        [
                          {
                            "id": "test-provider",
                            "name": "Test Provider",
                            "providerType": "openai",
                            "baseUrl": "{{env:TEST_BASE_URL}}",
                            "apiKey": "test-key",
                            "models": [
                              {
                                "id": "test-model",
                                "name": "Test Model"
                              }
                            ]
                          }
                        ]
                        """);
                TdAgentProperties properties = createProperties(catalog, "test-provider", "test-model");
                TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                        properties, objectMapper, resourceLoader);
                registry.initialize();

                TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

                // 如果环境变量未设置，应该返回null
                assertNull(config.getBaseUrl());
            } finally {
                // 恢复原始值（如果需要）
            }
        }

        @Test
        @DisplayName("应该解析apiKey中的环境变量占位符")
        void shouldResolveEnvPlaceholderInApiKey() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "test-provider",
                        "name": "Test Provider",
                        "providerType": "openai",
                        "baseUrl": "https://api.test.com/v1",
                        "apiKey": "{{env:DASHSCOPE_API_KEY}}",
                        "models": [
                          {
                            "id": "test-model",
                            "name": "Test Model"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "test-provider", "test-model");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            // 如果环境变量未设置，应该返回null，导致抛出API Key未配置的异常
            assertNull(config.getApiKey());
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("应该正确处理模型ID的大小写不匹配")
        void shouldHandleCaseInsensitiveModelIdMatching() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "dashscope",
                        "name": "DashScope",
                        "providerType": "dashscope",
                        "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "apiKey": "dash-key",
                        "models": [
                          {
                            "id": "Qwen-Max",
                            "name": "Qwen Max"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "dashscope", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("Qwen-Max", config.getModelId());
        }

        @Test
        @DisplayName("应该正确处理空白的providerId并使用默认值")
        void shouldUseDefaultProviderIdWhenBlank() throws Exception {
            Path catalog = createProviderCatalog("""
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
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "  ", "qwen-max");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertEquals("dashscope", config.getProviderId());
        }

        @Test
        @DisplayName("应该正确处理包含额外字段的供应商描述")
        void shouldHandleProviderWithExtraFields() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "advanced-provider",
                        "name": "Advanced Provider",
                        "providerType": "openai",
                        "baseUrl": "https://api.advanced.com/v1",
                        "apiKey": "advanced-key",
                        "endpointPath": "/v1/chat",
                        "formatter": "openai",
                        "apiKeyPrefix": "Bearer",
                        "isCustom": false,
                        "freezeUrl": true,
                        "supportModelDiscovery": true,
                        "supportConnectionCheck": false,
                        "generateKwargs": {
                          "temperature": 0.8
                        },
                        "models": [
                          {
                            "id": "advanced-model",
                            "name": "Advanced Model",
                            "supportsMultimodal": true,
                            "supportsVideo": false
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "advanced-provider", "advanced-model");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            assertNotNull(config);
            assertEquals("advanced-provider", config.getProviderId());
            assertEquals("/v1/chat", config.getEndpointPath());
        }

        @Test
        @DisplayName("应该正确处理嵌套的generateKwargs对象")
        void shouldHandleNestedGenerateKwargs() throws Exception {
            Path catalog = createProviderCatalog("""
                    [
                      {
                        "id": "nested-provider",
                        "name": "Nested Provider",
                        "providerType": "openai",
                        "baseUrl": "https://api.nested.com/v1",
                        "apiKey": "nested-key",
                        "generateKwargs": {
                          "outer": {
                            "inner": "value"
                          },
                          "array": [1, 2, 3]
                        },
                        "models": [
                          {
                            "id": "nested-model",
                            "name": "Nested Model"
                          }
                        ]
                      }
                    ]
                    """);
            TdAgentProperties properties = createProperties(catalog, "nested-provider", "nested-model");
            TdAgentProviderRegistry registry = new TdAgentProviderRegistry(
                    properties, objectMapper, resourceLoader);
            registry.initialize();

            TdAgentResolvedModelConfig config = registry.resolveConfiguredModel();

            Map<String, Object> additionalParams = config.getAdditionalBodyParams();
            assertNotNull(additionalParams.get("outer"));
            assertNotNull(additionalParams.get("array"));
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建临时的供应商目录文件
     *
     * @param content JSON内容
     * @return 目录文件路径
     * @throws Exception 文件写入异常
     */
    private Path createProviderCatalog(String content) throws Exception {
        Path catalog = tempDir.resolve("providers.json");
        Files.writeString(catalog, content);
        return catalog;
    }

    /**
     * 创建测试用的属性配置
     *
     * @param catalogPath 目录文件路径
     * @param providerId 供应商ID
     * @param modelId 模型ID
     * @return 配置对象
     */
    private TdAgentProperties createProperties(Path catalogPath, String providerId, String modelId) {
        TdAgentProperties properties = new TdAgentProperties();
        properties.getModel().setProviderConfigLocation(catalogPath.toUri().toString());
        properties.getModel().setProviderId(providerId);
        properties.getModel().setModelId(modelId);
        return properties;
    }
}
