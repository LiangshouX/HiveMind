package com.liangshou.tangdynasty.agentic.agents.provider.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.liangshou.tangdynasty.agentic.agents.provider.AbstractProvider;
import com.liangshou.tangdynasty.agentic.agents.provider.model.CheckResult;
import com.liangshou.tangdynasty.agentic.agents.provider.model.ModelInfo;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * OpenAI Provider 实现。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>基于 OpenAI 兼容 API（OpenAI / 任何 OpenAI-compatible 平台）提供连接性检查与模型发现能力</li>
 *     <li>根据指定 {@code modelId} 生成 AgentScope {@link OpenAIChatModel} 实例（用于后续对话推理）</li>
 * </ul>
 *
 * <p>约定：</p>
 * <ul>
 *     <li>{@link #baseUrl} 可填写形如 {@code https://api.openai.com} 或 {@code https://api.openai.com/v1}；
 *     本类会在必要时自动补全 {@code /v1}。</li>
 *     <li>鉴权使用 {@code Authorization: Bearer <apiKey>} 方式。</li>
 *     <li>{@link #generateKwargs} 中常见的生成参数会被映射到 {@link GenerateOptions}；其余键值会透传为
 *     {@link GenerateOptions.Builder#additionalBodyParam(String, Object)}。</li>
 * </ul>
 *
 * <p>线程安全：</p>
 * <ul>
 *     <li>{@link HttpClient} 为静态单例，可并发复用。</li>
 *     <li>当前 Provider 实例包含可变配置（如 apiKey/baseUrl/models），不要在多线程中并发修改同一实例的配置。</li>
 * </ul>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class OpenAIProvider extends AbstractProvider {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIProvider.class);

    /**
     * 共享 HTTP 客户端，用于调用 OpenAI 兼容 REST API。
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    /**
     * 默认 OpenAI Base URL（包含 {@code /v1}）。
     */
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    /**
     * 构造一个 OpenAIProvider，并可选择初始化内建默认配置。
     *
     * <p>当 {@code initDefaults=true} 时，将填充如下默认值：</p>
     * <ul>
     *     <li>{@code id=openai}</li>
     *     <li>{@code name=OpenAI}</li>
     *     <li>{@code baseUrl=https://api.openai.com/v1}</li>
     *     <li>{@code apiKeyPrefix=sk-}</li>
     *     <li>{@code chatModel=io.agentscope.core.model.OpenAIChatModel}</li>
     *     <li>开启模型发现与连接性检查标识</li>
     * </ul>
     *
     * @param initDefaults 是否写入默认配置
     */
    public OpenAIProvider(boolean initDefaults) {
        if (!initDefaults) return;
        this.setId("openai");
        this.setName("OpenAI");
        this.setBaseUrl(DEFAULT_BASE_URL);
        this.setApiKeyPrefix("sk-");
        this.setChatModel(OpenAIChatModel.class.getName());
        this.setLocal(false);
        this.setCustom(false);
        this.setFreezeUrl(false);
        this.setRequireApiKey(true);
        this.setSupportModelDiscovery(true);
        this.setSupportConnectionCheck(true);
        if (this.getGenerateKwargs() == null) {
            this.setGenerateKwargs(Map.of());
        }
    }

    /**
     * 检查当前 Provider 配置下 OpenAI 兼容平台的连通性（不依赖具体模型）。
     *
     * <p>实现策略：调用 {@code GET /v1/models}，HTTP 2xx 视为可用。</p>
     *
     * @param timeout 本次检查的超时时间（为空时默认 30 秒）
     * @return 异步返回检查结果，ok=true 表示连通
     */
    @Override
    public CompletableFuture<CheckResult> checkConnection(Duration timeout) {
        String apiKeyError = validateApiKey();
        if (apiKeyError != null) {
            return CompletableFuture.completedFuture(CheckResult.error(apiKeyError));
        }

        String url = buildUrl(this.getBaseUrl(), "/models");
        HttpRequest request = baseRequestBuilder(url, timeout).GET().build();

        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((resp, ex) -> {
                    if (ex != null) {
                        return CheckResult.error("Failed to connect: " + ex.getMessage());
                    }
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return CheckResult.ok();
                    }
                    return CheckResult.error("Connection check failed: HTTP " + resp.statusCode() + " | " + safeBody(resp.body()));
                });
    }

    /**
     * 从 OpenAI 兼容平台拉取可用模型列表。
     *
     * <p>实现策略：调用 {@code GET /v1/models} 并解析响应中的 {@code data[].id}。</p>
     *
     * <p>注意：若 apiKey 缺失或请求失败，返回空列表且不会抛异常。</p>
     *
     * @param timeout 本次请求超时时间（为空时默认 30 秒）
     * @return 异步返回 {@link ModelInfo} 列表
     */
    @Override
    public CompletableFuture<List<ModelInfo>> fetchModels(Duration timeout) {
        String apiKeyError = validateApiKey();
        if (apiKeyError != null) {
            return CompletableFuture.completedFuture(List.of());
        }

        String url = buildUrl(this.getBaseUrl(), "/models");
        HttpRequest request = baseRequestBuilder(url, timeout).GET().build();

        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(resp -> {
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        LOGGER.warn("Failed to fetch models: HTTP {} | {}", resp.statusCode(), safeBody(resp.body()));
                        return List.<ModelInfo>of();
                    }

                    try {
                        JSONObject json = JSON.parseObject(resp.body());
                        JSONArray data = json == null ? null : json.getJSONArray("data");
                        if (data == null) {
                            return List.<ModelInfo>of();
                        }
                        List<ModelInfo> models = new ArrayList<>(data.size());
                        for (int i = 0; i < data.size(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            if (item == null) continue;
                            String id = item.getString("id");
                            if (id == null || id.isBlank()) continue;
                            models.add(ModelInfo.builder().id(id).name(id).build());
                        }
                        this.setModels(models);
                        return models;
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse model list: {}", e.getMessage());
                        return List.<ModelInfo>of();
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.warn("Failed to fetch models: {}", ex.getMessage());
                    return List.of();
                });
    }

    /**
     * 检查指定模型在当前平台上的可用性。
     *
     * <p>实现策略：</p>
     * <ol>
     *     <li>优先调用 {@code GET /v1/models/{modelId}}，若 2xx 则认为模型存在且可用</li>
     *     <li>若上述失败，则 fallback 调用 {@code POST /v1/chat/completions} 做一次最小探测</li>
     * </ol>
     *
     * @param modelId 模型标识（OpenAI API 使用的 model 字段）
     * @param timeout 本次检查超时时间（为空时默认 30 秒）
     * @return 异步返回检查结果
     */
    @Override
    public CompletableFuture<CheckResult> checkModelConnection(String modelId, Duration timeout) {
        if (modelId == null || modelId.isBlank()) {
            return CompletableFuture.completedFuture(CheckResult.error("modelId is empty"));
        }
        String apiKeyError = validateApiKey();
        if (apiKeyError != null) {
            return CompletableFuture.completedFuture(CheckResult.error(apiKeyError));
        }

        String url = buildUrl(this.getBaseUrl(), "/models/" + modelId);
        HttpRequest getRequest = baseRequestBuilder(url, timeout).GET().build();

        return HTTP_CLIENT
                .sendAsync(getRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenCompose(resp -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return CompletableFuture.completedFuture(CheckResult.ok());
                    }
                    return probeChatCompletion(modelId, timeout);
                })
                .exceptionally(ex -> CheckResult.error("Model connection check failed: " + ex.getMessage()));
    }

    /**
     * 获取指定模型对应的 AgentScope {@link OpenAIChatModel} 实例。
     *
     * <p>该实例会携带：</p>
     * <ul>
     *     <li>apiKey：来自 {@link #getApiKey()}</li>
     *     <li>baseUrl：来自 {@link #getBaseUrl()}（若为空则使用 SDK 默认）</li>
     *     <li>generate options：由 {@link #getGenerateKwargs()} 映射构建</li>
     * </ul>
     *
     * @param modelId 模型标识
     * @return {@link OpenAIChatModel} 实例（返回类型为 Object 以匹配抽象层定义）
     * @throws IllegalArgumentException modelId 为空
     * @throws IllegalStateException    apiKey 缺失且当前 Provider 要求 apiKey
     */
    @Override
    public Object getChatModelInstance(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId is empty");
        }
        String apiKeyError = validateApiKey();
        if (apiKeyError != null) {
            throw new IllegalStateException(apiKeyError);
        }

        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(this.getApiKey())
                .modelName(modelId);

        String baseUrl = this.getBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        Map<String, Object> kwargs = this.getGenerateKwargs();
        if (kwargs != null && !kwargs.isEmpty()) {
            builder.generateOptions(toGenerateOptions(kwargs));
            Object stream = firstNonNull(kwargs.get("stream"), kwargs.get("stream_enabled"), kwargs.get("streamEnabled"));
            if (stream instanceof Boolean b) {
                builder.stream(b);
            }
        }

        return builder.build();
    }

    /**
     * 使用 {@code POST /v1/chat/completions} 对指定模型做最小探测。
     *
     * <p>该探测会发送一条简单用户消息，并限制 {@code max_tokens=1}，用于验证模型是否可完成一次对话请求。</p>
     *
     * @param modelId 模型标识
     * @param timeout 超时时间（为空时默认 30 秒）
     * @return 异步返回检查结果
     */
    private CompletableFuture<CheckResult> probeChatCompletion(String modelId, Duration timeout) {
        String url = buildUrl(this.getBaseUrl(), "/chat/completions");
        JSONObject payload = new JSONObject();
        payload.put("model", modelId);
        payload.put("stream", false);
        payload.put("max_tokens", 1);
        payload.put("messages", List.of(Map.of("role", "user", "content", "ping")));

        HttpRequest request = baseRequestBuilder(url, timeout)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString(), StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((resp, ex) -> {
                    if (ex != null) {
                        return CheckResult.error("Model connection check failed: " + ex.getMessage());
                    }
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return CheckResult.ok();
                    }
                    return CheckResult.error("Model connection check failed: HTTP " + resp.statusCode() + " | " + safeBody(resp.body()));
                });
    }

    /**
     * 构造带默认请求头的 {@link HttpRequest.Builder}。
     *
     * <p>默认 headers：</p>
     * <ul>
     *     <li>{@code Accept: application/json}</li>
     *     <li>{@code Authorization: Bearer <apiKey>}</li>
     * </ul>
     *
     * @param url     完整请求 URL
     * @param timeout 超时时间（为空时默认 30 秒）
     * @return 请求构建器
     */
    private HttpRequest.Builder baseRequestBuilder(String url, Duration timeout) {
        Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(effectiveTimeout)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + this.getApiKey());
    }

    /**
     * 校验 apiKey 是否满足当前 Provider 的约束。
     *
     * @return 若通过返回 null；否则返回可读的错误信息
     */
    private String validateApiKey() {
        if (!this.isRequireApiKey()) return null;
        String apiKey = this.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "API key is required for provider '" + this.getName() + "'";
        }
        return null;
    }

    /**
     * 生成 OpenAI 兼容 API 的完整 URL。
     *
     * <p>兼容如下输入：</p>
     * <ul>
     *     <li>{@code baseUrl=https://api.openai.com} + {@code endpoint=/models} → {@code https://api.openai.com/v1/models}</li>
     *     <li>{@code baseUrl=https://api.openai.com/v1} + {@code endpoint=/models} → {@code https://api.openai.com/v1/models}</li>
     *     <li>{@code baseUrl=https://xxx.com/custom/v4} + {@code endpoint=/models} → {@code https://xxx.com/custom/v4/models}</li>
     * </ul>
     *
     * @param baseUrl      Provider 配置的 baseUrl，可为空
     * @param endpointPath 端点路径（建议以 "/" 开头；若不以 "/" 开头会自动补齐）
     * @return 拼接后的完整 URL
     */
    private String buildUrl(String baseUrl, String endpointPath) {
        String base = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        boolean hasVersionSuffix = base.matches(".*/v\\d+$");
        String endpoint = endpointPath == null ? "" : endpointPath.trim();
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }

        if (!hasVersionSuffix) {
            base = base + "/v1";
        }
        return base + endpoint;
    }

    /**
     * 将 Provider 的 {@link #generateKwargs} 转换为 AgentScope {@link GenerateOptions}。
     *
     * <p>当前会映射的常见键（支持 snake_case 与 camelCase 混用）：</p>
     * <ul>
     *     <li>temperature / temp</li>
     *     <li>top_p / topP</li>
     *     <li>max_tokens / maxTokens</li>
     *     <li>frequency_penalty / frequencyPenalty</li>
     *     <li>presence_penalty / presencePenalty</li>
     *     <li>top_k / topK</li>
     *     <li>seed</li>
     * </ul>
     *
     * <p>其余键值会通过 {@link GenerateOptions.Builder#additionalBodyParam(String, Object)} 透传到请求体。</p>
     * <p>以下键仅用于配置 SDK 行为，不会被透传为 body 参数：stream / stream_enabled / streamEnabled。</p>
     *
     * @param kwargs 生成参数字典
     * @return {@link GenerateOptions}（不可变）
     */
    private GenerateOptions toGenerateOptions(Map<String, Object> kwargs) {
        GenerateOptions.Builder builder = GenerateOptions.builder();
        Set<String> consumed = new HashSet<>();

        applyDouble(kwargs, builder::temperature, consumed, "temperature", "temp");
        applyDouble(kwargs, builder::topP, consumed, "top_p", "topP");
        applyInteger(kwargs, builder::maxTokens, consumed, "max_tokens", "maxTokens");
        applyDouble(kwargs, builder::frequencyPenalty, consumed, "frequency_penalty", "frequencyPenalty");
        applyDouble(kwargs, builder::presencePenalty, consumed, "presence_penalty", "presencePenalty");
        applyInteger(kwargs, builder::topK, consumed, "top_k", "topK");
        applyLong(kwargs, builder::seed, consumed, "seed");

        for (Map.Entry<String, Object> entry : kwargs.entrySet()) {
            String k = entry.getKey();
            if (k == null || consumed.contains(k)) continue;
            if (Objects.equals(k, "stream") || Objects.equals(k, "stream_enabled") || Objects.equals(k, "streamEnabled")) {
                continue;
            }
            builder.additionalBodyParam(k, entry.getValue());
        }

        return builder.build();
    }

    /**
     * 从字典中读取一个 Double 参数并设置到 {@link GenerateOptions.Builder}。
     *
     * @param kwargs    生成参数字典
     * @param setter   builder 的 setter
     * @param consumed 已消费 key 集合，用于避免重复透传
     * @param keys     支持的 key 别名（按顺序取第一个非空值）
     */
    private void applyDouble(
            Map<String, Object> kwargs,
            java.util.function.Consumer<Double> setter,
            Set<String> consumed,
            String... keys) {
        Object v = firstNonNull(kwargs, keys);
        Double d = toDouble(v);
        if (d == null) return;
        setter.accept(d);
        consumed.addAll(List.of(keys));
    }

    /**
     * 从字典中读取一个 Integer 参数并设置到 {@link GenerateOptions.Builder}。
     *
     * @param kwargs    生成参数字典
     * @param setter   builder 的 setter
     * @param consumed 已消费 key 集合，用于避免重复透传
     * @param keys     支持的 key 别名（按顺序取第一个非空值）
     */
    private void applyInteger(
            Map<String, Object> kwargs,
            IntConsumer setter,
            Set<String> consumed,
            String... keys) {
        Object v = firstNonNull(kwargs, keys);
        Integer i = toInteger(v);
        if (i == null) return;
        setter.accept(i);
        consumed.addAll(List.of(keys));
    }

    /**
     * 从字典中读取一个 Long 参数并设置到 {@link GenerateOptions.Builder}。
     *
     * @param kwargs    生成参数字典
     * @param setter   builder 的 setter
     * @param consumed 已消费 key 集合，用于避免重复透传
     * @param keys     支持的 key 别名（按顺序取第一个非空值）
     */
    private void applyLong(
            Map<String, Object> kwargs,
            LongConsumer setter,
            Set<String> consumed,
            String... keys) {
        Object v = firstNonNull(kwargs, keys);
        Long l = toLong(v);
        if (l == null) return;
        setter.accept(l);
        consumed.addAll(List.of(keys));
    }

    /**
     * 从字典中读取一个 Boolean 参数并设置到 builder。
     *
     * @param kwargs    生成参数字典
     * @param setter   setter
     * @param consumed 已消费 key 集合
     * @param keys     支持的 key 别名
     */
    private void applyBoolean(
            Map<String, Object> kwargs,
            java.util.function.Consumer<Boolean> setter,
            Set<String> consumed,
            String... keys) {
        Object v = firstNonNull(kwargs, keys);
        Boolean b = toBoolean(v);
        if (b == null) return;
        setter.accept(b);
        consumed.addAll(List.of(keys));
    }

    /**
     * 从字典中读取一个 String 参数并设置到 builder。
     *
     * @param kwargs    生成参数字典
     * @param setter   setter
     * @param consumed 已消费 key 集合
     * @param keys     支持的 key 别名
     */
    private void applyString(
            Map<String, Object> kwargs,
            java.util.function.Consumer<String> setter,
            Set<String> consumed,
            String... keys) {
        Object v = firstNonNull(kwargs, keys);
        if (v == null) return;
        setter.accept(String.valueOf(v));
        consumed.addAll(List.of(keys));
    }

    /**
     * 从字典中按 key 别名顺序取第一个非空值。
     *
     * @param map  参数字典
     * @param keys key 别名列表
     * @return 第一个非空值；若都为空返回 null
     */
    private Object firstNonNull(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) return null;
        for (String k : keys) {
            if (k == null) continue;
            Object v = map.get(k);
            if (v != null) return v;
        }
        return null;
    }

    /**
     * 从可变参数中按顺序取第一个非空值。
     *
     * @param values 值列表
     * @return 第一个非空值；若都为空返回 null
     */
    private Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }

    /**
     * 尝试将输入转换为 Double。
     *
     * @param v 输入对象
     * @return 可解析返回 Double，否则返回 null
     */
    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 尝试将输入转换为 Integer。
     *
     * @param v 输入对象
     * @return 可解析返回 Integer，否则返回 null
     */
    private Integer toInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 尝试将输入转换为 Long。
     *
     * @param v 输入对象
     * @return 可解析返回 Long，否则返回 null
     */
    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 尝试将输入转换为 Boolean。
     *
     * <p>支持的真值：true/1/yes；假值：false/0/no（大小写不敏感）。</p>
     *
     * @param v 输入对象
     * @return 可解析返回 Boolean，否则返回 null
     */
    private Boolean toBoolean(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim().toLowerCase();
        if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) return true;
        if ("false".equals(s) || "0".equals(s) || "no".equals(s)) return false;
        return null;
    }

    /**
     * 用于日志展示的安全响应体截断。
     *
     * @param body 原始响应体
     * @return 不超过 500 字符的响应体（超出部分以省略号结尾）
     */
    private String safeBody(String body) {
        if (body == null) return "";
        if (body.length() <= 500) return body;
        return body.substring(0, 500) + "...";
    }
}
