package com.liangshou.agentic.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 活跃模型响应 - 返回用户已激活的供应商及其模型列表。
 *
 * <p>用于 {@code GET /api/v1/tdagent/active-models} 端点，
 * 查询数据库中用户已激活的供应商，并结合内置供应商目录返回完整的模型信息。</p>
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class ActiveModelsResponse {

    /**
     * 已激活的供应商列表
     */
    private List<ActiveProvider> providers;

    /**
     * 活跃供应商信息
     */
    @Getter
    @Builder
    public static class ActiveProvider {

        /** 供应商 ID（内置目录中的标识） */
        private String providerId;

        /** 供应商显示名称 */
        private String providerName;

        /** 供应商类型（DASHSCOPE / OPENAI） */
        private String providerType;

        /** 该供应商下的可用模型列表 */
        private List<ActiveModel> models;
    }

    /**
     * 活跃模型信息
     */
    @Getter
    @Builder
    public static class ActiveModel {

        /** 模型 ID */
        private String modelId;

        /** 模型显示名称 */
        private String modelName;

        /** 是否支持多模态输入 */
        private boolean supportsMultimodal;

        /** 是否支持视频输入 */
        private boolean supportsVideo;
    }
}
