package com.liangshou.agentic.infrastructure.mysql.support;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Token 单价表 - 按模型（及供应商）维护输入/输出 Token 的计费单价。
 *
 * <p>单价通过配置文件 {@code tdagent.token-pricing} 外部化，单位为
 * <b>元 / 百万 Token</b>（与主流 LLM 厂商公开报价的计价口径一致）。</p>
 *
 * <p>配置格式：</p>
 * <pre>{@code
 * tdagent:
 *   token-pricing:
 *     enabled: true
 *     currency: CNY
 *     models:
 *       - provider: dashscope
 *         model: qwen-max
 *         input: 2.4
 *         output: 9.6
 *       - provider: deepseek
 *         model: deepseek-chat
 *         input: 2.0
 *         output: 8.0
 * }</pre>
 *
 * <p>匹配规则：先按 {@code provider + model} 精确匹配，未命中时退化为按
 * {@code model} 匹配，再未命中则按 {@code provider} 默认价匹配。全部未命中时
 * 费用记为 0，并在日志中提示缺失单价。</p>
 *
 * @author LiangshouX
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "tdagent.token-pricing")
public class TokenPricing {

    /**
     * 单价条目：某个模型（或某供应商默认）的输入/输出单价，单位元/百万 Token。
     */
    @Getter
    @Setter
    public static class Pricing {
        /** 供应商 ID，如 dashscope / deepseek；可为空表示仅按模型匹配。 */
        private String provider;
        /** 模型 ID，如 qwen-max；可为空表示该供应商的默认价。 */
        private String model;
        /** 输入 Token 单价（元 / 百万 Token）。 */
        private BigDecimal input = BigDecimal.ZERO;
        /** 输出 Token 单价（元 / 百万 Token）。 */
        private BigDecimal output = BigDecimal.ZERO;
    }

    /** 每百万 Token 的换算系数。 */
    private static final BigDecimal PER_MILLION = new BigDecimal("1000000");

    /** 是否启用费用估算。 */
    private boolean enabled = true;

    /** 币种符号。 */
    private String currency = "CNY";

    /** 单价条目列表。 */
    private List<Pricing> models = List.of();

    /** provider+model -> pricing 的精确索引。 */
    private final Map<String, Pricing> exactIndex = new HashMap<>();
    /** model -> pricing 的次级索引。 */
    private final Map<String, Pricing> modelIndex = new HashMap<>();
    /** provider -> pricing 的兜底索引。 */
    private final Map<String, Pricing> providerIndex = new HashMap<>();

    /**
     * 配置绑定完成后建立匹配索引。
     */
    @PostConstruct
    void buildIndexes() {
        exactIndex.clear();
        modelIndex.clear();
        providerIndex.clear();
        for (Pricing pricing : models) {
            if (pricing == null) {
                continue;
            }
            if (isNotBlank(pricing.getProvider()) && isNotBlank(pricing.getModel())) {
                exactIndex.put(key(pricing.getProvider(), pricing.getModel()), pricing);
            }
            if (isNotBlank(pricing.getModel())) {
                modelIndex.putIfAbsent(pricing.getModel(), pricing);
            }
            if (isNotBlank(pricing.getProvider())) {
                providerIndex.putIfAbsent(pricing.getProvider(), pricing);
            }
        }
        log.info("[TokenPricing] 单价表加载完成 - currency={}, entries={}", currency, models.size());
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String key(String provider, String model) {
        return provider.trim().toLowerCase(java.util.Locale.ROOT) + "::" + model.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 计算一次调用的估算费用。
     *
     * @param provider     供应商 ID
     * @param model        模型 ID
     * @param inputTokens  输入 Token 数
     * @param outputTokens 输出 Token 数
     * @return 估算费用（元），未配置单价时返回 0
     */
    public BigDecimal estimate(String provider, String model, long inputTokens, long outputTokens) {
        if (!enabled) {
            return BigDecimal.ZERO;
        }
        Pricing pricing = resolve(provider, model);
        if (pricing == null) {
            log.warn("[TokenPricing] 未找到模型单价 - provider={}, model={}", provider, model);
            return BigDecimal.ZERO;
        }
        BigDecimal cost = pricing.getInput()
                .multiply(BigDecimal.valueOf(Math.max(inputTokens, 0)))
                .add(pricing.getOutput().multiply(BigDecimal.valueOf(Math.max(outputTokens, 0))))
                .divide(PER_MILLION, 6, RoundingMode.HALF_UP);
        return cost.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按 provider+model → model → provider 的顺序解析单价条目。
     */
    private Pricing resolve(String provider, String model) {
        if (isNotBlank(provider) && isNotBlank(model)) {
            Pricing exact = exactIndex.get(key(provider, model));
            if (exact != null) {
                return exact;
            }
        }
        if (isNotBlank(model)) {
            Pricing byModel = modelIndex.get(model);
            if (byModel != null) {
                return byModel;
            }
        }
        if (isNotBlank(provider)) {
            return providerIndex.get(provider);
        }
        return null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency == null || currency.isBlank() ? "CNY" : currency;
    }

    public List<Pricing> getModels() {
        return models;
    }

    public void setModels(List<Pricing> models) {
        this.models = models == null ? List.of() : models;
    }
}
