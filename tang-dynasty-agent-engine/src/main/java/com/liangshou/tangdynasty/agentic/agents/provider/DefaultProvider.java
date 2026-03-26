package com.liangshou.tangdynasty.agentic.agents.provider;

import com.liangshou.tangdynasty.agentic.agents.provider.model.CheckResult;
import com.liangshou.tangdynasty.agentic.agents.provider.model.ModelInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DefaultProvider：默认 Provider 实现。
 * <p>
 * - 无实际 ChatModel 实例化能力
 * - 连接性与模型检查基于列表存在性判断
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class DefaultProvider extends AbstractProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProvider.class);

    /**
     * 若存在至少一个模型条目则认为“可用”，否则返回失败。
     */
    @Override
    public CompletableFuture<CheckResult> checkConnection(Duration timeout) {
        return CompletableFuture.supplyAsync(() -> !this.getModels().isEmpty() ? CheckResult.ok() : CheckResult.error("No models available in the default provider"));
    }

    /**
     * 直接返回当前 models 列表。
     */
    @Override
    public CompletableFuture<List<ModelInfo>> fetchModels(Duration timeout) {
        return CompletableFuture.completedFuture(this.getModels());
    }

    /**
     * 基于是否存在指定 modelId 判断可用性。
     */
    @Override
    public CompletableFuture<CheckResult> checkModelConnection(String modelId, Duration timeout) {
        return CompletableFuture.supplyAsync(() -> hasModel(modelId) ? CheckResult.ok() : CheckResult.error("Model '" + modelId + "' not found"));
    }

    /**
     * 默认实现不改动配置。
     */
    @Override
    public void updateConfig(java.util.Map<String, Object> config) {
        LOGGER.info("DefaultProvider does not support updating config");
    }

    /**
     * 默认实现抛出不支持异常。
     */
    @Override
    public Object getChatModelInstance(String modelId) {
        throw new UnsupportedOperationException("DefaultProvider does not implement chat model");
    }
}
