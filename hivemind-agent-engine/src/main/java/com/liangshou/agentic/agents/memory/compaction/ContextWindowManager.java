package com.liangshou.agentic.agents.memory.compaction;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.agentic.agents.provider.TdAgentResolvedModelConfig;
import com.liangshou.agentic.common.config.TdAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 上下文窗口管理器 - 管理模型上下文窗口配置和压缩阈值计算。
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>解析当前会话所用模型的上下文窗口大小</li>
 *     <li>计算压缩触发阈值（默认 85%）</li>
 *     <li>计算输出预留空间</li>
 *     <li>判断当前上下文是否需要压缩</li>
 * </ul>
 *
 * <p><b>上下文窗口解析优先级</b>：</p>
 * <ol>
 *     <li><b>模型级</b>：内置供应商目录（{@code builtin_provider.json}）或用户自建供应商
 *         {@code models_json} 中为该模型声明的 {@code contextWindow}</li>
 *     <li><b>全局配置</b>：{@code tdagent.compaction.context-window-size}（0 表示未配置）</li>
 *     <li><b>内置默认值</b>：按模型名匹配的兜底表</li>
 *     <li><b>最终默认值</b>：{@value #DEFAULT_CONTEXT_WINDOW}</li>
 * </ol>
 *
 * @author LiangshouX
 */
@Component
public class ContextWindowManager {

    private static final Logger log = LoggerFactory.getLogger(ContextWindowManager.class);

    private static final double COMPACTION_THRESHOLD_RATIO = 0.85;
    private static final int DEFAULT_CONTEXT_WINDOW = 32768;

    private final TdAgentProperties properties;
    private final TdAgentProviderRegistry providerRegistry;

    public ContextWindowManager(
            TdAgentProperties properties,
            TdAgentProviderRegistry providerRegistry) {
        this.properties = properties;
        this.providerRegistry = providerRegistry;
    }

    /**
     * 获取当前模型的上下文窗口大小（无会话上下文时按全局配置解析）。
     *
     * @return 上下文窗口大小（tokens）
     */
    public int getContextWindowSize() {
        int configured = properties.getCompaction().getContextWindowSize();
        if (configured > 0) {
            return configured;
        }
        return resolveByModelName(properties.getModel().getModelId());
    }

    /**
     * 获取指定会话上下文对应的上下文窗口大小。
     *
     * @param context 会话上下文，可为 null
     * @return 上下文窗口大小（tokens）
     */
    public int getContextWindowSize(ConversationSessionContext context) {
        if (context == null) {
            return getContextWindowSize();
        }
        // 1. 模型级声明优先（内置供应商目录 / 用户自建供应商）
        Integer modelWindow = resolveModelContextWindow(context);
        if (modelWindow != null && modelWindow > 0) {
            return modelWindow;
        }
        // 2. 全局配置
        int configured = properties.getCompaction().getContextWindowSize();
        if (configured > 0) {
            return configured;
        }
        // 3. 内置默认值
        return resolveByModelName(context.getModelId());
    }

    /**
     * 获取压缩触发阈值（token 数）。
     *
     * @return 触发压缩所需的最小 token 数
     */
    public int getCompactionThreshold() {
        return getCompactionThreshold(null);
    }

    /**
     * 获取指定会话的压缩触发阈值（token 数）。
     *
     * @param context 会话上下文，可为 null
     * @return 触发压缩所需的最小 token 数
     */
    public int getCompactionThreshold(ConversationSessionContext context) {
        return (int) (getContextWindowSize(context) * getThresholdRatio());
    }

    /**
     * 获取压缩阈值比例。
     *
     * @return 上下文窗口的百分比（0~1）
     */
    public double getThresholdRatio() {
        double ratio = properties.getCompaction().getThresholdRatio();
        if (ratio <= 0 || ratio > 1) {
            ratio = COMPACTION_THRESHOLD_RATIO;
        }
        return ratio;
    }

    /**
     * 获取输出预留空间（token 数）。
     *
     * @return 输出预留 token 数
     */
    public int getOutputReserve() {
        int configured = properties.getCompaction().getOutputReserveTokens();
        if (configured > 0) {
            return configured;
        }
        return 8192; // 默认预留 max_tokens
    }

    /**
     * 判断当前上下文是否需要压缩。
     *
     * @param currentTokens 当前上下文总 token 数
     * @return true 如果需要压缩
     */
    public boolean needsCompaction(int currentTokens) {
        return needsCompaction(null, currentTokens);
    }

    /**
     * 判断指定会话的当前上下文是否需要压缩。
     *
     * @param context       会话上下文，可为 null
     * @param currentTokens 当前上下文总 token 数
     * @return true 如果需要压缩
     */
    public boolean needsCompaction(ConversationSessionContext context, int currentTokens) {
        return currentTokens >= getCompactionThreshold(context);
    }

    /**
     * 计算压缩目标 token 数（压缩后应达到的值）。
     *
     * @return 压缩目标 token 数
     */
    public int getCompactionTarget() {
        return getCompactionTarget(null);
    }

    /**
     * 计算指定会话的压缩目标 token 数（压缩后应达到的值）。
     *
     * @param context 会话上下文，可为 null
     * @return 压缩目标 token 数
     */
    public int getCompactionTarget(ConversationSessionContext context) {
        return (int) (getCompactionThreshold(context) * 0.7);
    }

    /**
     * 解析模型描述符中声明的上下文窗口。
     */
    private Integer resolveModelContextWindow(ConversationSessionContext context) {
        // 会话级模型快照优先（解析时已锁定本轮使用的模型）
        TdAgentResolvedModelConfig resolved = context.getResolvedModelConfig();
        if (resolved != null && resolved.getContextWindow() != null && resolved.getContextWindow() > 0) {
            return resolved.getContextWindow();
        }
        try {
            String providerId = context.getProviderId() != null
                    ? context.getProviderId()
                    : (resolved == null ? null : resolved.getProviderId());
            String modelId = context.getModelId() != null
                    ? context.getModelId()
                    : (resolved == null ? null : resolved.getModelId());
            return providerRegistry.resolveContextWindow(context.getUserId(), providerId, modelId);
        } catch (Exception e) {
            log.debug("[ContextWindowManager] 解析模型上下文窗口失败 - error: {}", e.getMessage());
            return null;
        }
    }

    private int resolveByModelName(String modelId) {
        if (modelId == null) {
            return DEFAULT_CONTEXT_WINDOW;
        }
        return switch (modelId.toLowerCase()) {
            case "qwen-max" -> 32768;
            case "qwen3-max", "qwen3.5-plus", "qwen3.5", "qwen-plus" -> 131072;
            case "deepseek-chat", "deepseek-reasoner" -> 65536;
            default -> DEFAULT_CONTEXT_WINDOW;
        };
    }
}
