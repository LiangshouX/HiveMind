package com.liangshou.tangdynasty.agentic.agents.provider.old.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModelInfo：模型信息数据结构。
 * <p>
 * 对应 Provider 下的单个模型条目，包含功能能力标注与来源。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    /**
     * 模型标识（API 使用的 ID）
     */
    private String id;

    /**
     * 模型名称（人类可读）
     */
    private String name;

    /**
     * 是否支持多模态（为空表示尚未探针）
     */
    private Boolean supportsMultimodal;

    /**
     * 是否支持图片输入（为空表示尚未探针）
     */
    private Boolean supportsImage;

    /**
     * 是否支持视频输入（为空表示尚未探针）
     */
    private Boolean supportsVideo;

    /**
     * 能力来源（documentation/probed 等）
     */
    private String probeSource;
}
