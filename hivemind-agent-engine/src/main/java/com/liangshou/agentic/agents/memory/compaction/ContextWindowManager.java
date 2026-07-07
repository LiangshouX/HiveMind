package com.liangshou.agentic.agents.memory.compaction;

import com.liangshou.agentic.common.config.TdAgentProperties;
import org.springframework.stereotype.Component;

/**
 * 上下文窗口管理器 - 管理模型上下文窗口配置和压缩阈值计算。
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>维护每个模型的上下文窗口大小配置</li>
 *     <li>计算压缩触发阈值（默认 85%）</li>
 *     <li>计算输出预留空间</li>
 *     <li>判断当前上下文是否需要压缩</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
public class ContextWindowManager {

    private static final double COMPACTION_THRESHOLD_RATIO = 0.85;
    private static final int DEFAULT_CONTEXT_WINDOW = 32768;

    private final TdAgentProperties properties;

    public ContextWindowManager(TdAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取当前模型的上下文窗口大小。
     *
     * @return 上下文窗口大小（tokens）
     */
    public int getContextWindowSize() {
        int configured = properties.getCompaction().getContextWindowSize();
        if (configured > 0) {
            return configured;
        }
        String modelId = properties.getModel().getModelId();
        return resolveContextWindow(modelId);
    }

    /**
     * 获取压缩触发阈值（token 数）。
     *
     * @return 触发压缩所需的最小 token 数
     */
    public int getCompactionThreshold() {
        double ratio = properties.getCompaction().getThresholdRatio();
        if (ratio <= 0 || ratio > 1) {
            ratio = COMPACTION_THRESHOLD_RATIO;
        }
        return (int) (getContextWindowSize() * ratio);
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
        return currentTokens >= getCompactionThreshold();
    }

    /**
     * 计算压缩目标 token 数（压缩后应达到的值）。
     *
     * @return 压缩目标 token 数
     */
    public int getCompactionTarget() {
        return (int) (getCompactionThreshold() * 0.7);
    }

    private int resolveContextWindow(String modelId) {
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
