package com.liangshou.service.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 内置供应商目录条目 DTO。
 *
 * <p>用于「模型配置」页的快捷配置流程：用户从清单中选择一个供应商，
 * 系统自动填入 Base URL 与模型列表，用户只需填写自己的 API Key。</p>
 *
 * <p>注意：出于安全考虑，目录中<b>不包含任何 API Key</b>，
 * 平台也不托管密钥 —— 每个用户使用自己的 Key。</p>
 */
@Data
public class ProviderCatalogDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Provider 标识（如 dashscope），创建时作为 modelProviderId 使用。 */
    private String id;

    /** 供应商显示名称。 */
    private String name;

    /** 供应商简介。 */
    private String description;

    /** API 端点地址。 */
    private String baseUrl;

    /** 供应商类型：SYSTEM / CUSTOM / LOCAL。 */
    private String providerType;

    /** API Key 前缀提示（如 sk-），用于输入框占位提示。 */
    private String apiKeyPrefix;

    /** 官方控制台地址，供用户前往申请 API Key。 */
    private String consoleUrl;

    /** 申请 API Key 的直达地址（可为空）。 */
    private String apiKeyUrl;

    /** 是否需要填写 API Key（本地部署等场景为 false）。 */
    private Boolean apiKeyRequired = true;

    /** 该供应商的常用模型列表。 */
    private List<CatalogModel> models;

    /**
     * 目录中的模型条目。
     */
    @Data
    public static class CatalogModel implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 模型 ID（API 调用时使用）。 */
        private String id;

        /** 模型显示名称。 */
        private String name;

        /** 是否支持多模态（图片输入）。 */
        private Boolean supportsMultimodal;

        /** 是否支持视频输入。 */
        private Boolean supportsVideo;

        /**
         * 模型的上下文窗口大小（tokens）。
         *
         * <p>来自内置供应商目录中为该模型声明的 {@code contextWindow}；
         * 未声明时为 null，前端留空即使用系统默认值。</p>
         */
        private Integer contextWindow;
    }
}
