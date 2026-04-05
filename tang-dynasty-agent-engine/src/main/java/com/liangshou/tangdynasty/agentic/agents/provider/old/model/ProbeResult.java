package com.liangshou.tangdynasty.agentic.agents.provider.old.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProbeResult：模型多模态能力探针结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProbeResult {
    /**
     * 是否支持多模态
     */
    private boolean supportsMultimodal;

    /**
     * 是否支持图片
     */
    private boolean supportsImage;

    /**
     * 是否支持视频
     */
    private boolean supportsVideo;

    /**
     * 探针来源描述
     */
    private String source;
}
