package com.liangshou.tangdynasty.agentic.agents.provider;

import lombok.Getter;
import lombok.Setter;

/**
 * 模型描述符 - 描述 LLM 模型的基本信息和能力。
 *
 * <p>该对象包含以下字段：</p>
 * <ul>
 *     <li>{@code id} - 模型唯一标识，如 "qwen-max"、"gpt-4"</li>
 *     <li>{@code name} - 模型显示名称，用于 UI 展示</li>
 *     <li>{@code supportsMultimodal} - 是否支持多模态输入（图片、音频等）</li>
 *     <li>{@code supportsVideo} - 是否支持视频输入</li>
 *     <li>{@code probeSource} - 模型探测数据来源，用于健康检查</li>
 * </ul>
 *
 * <p>该描述符由 {@link TdAgentProviderRegistry} 从配置文件中加载，用于前端展示可用模型列表。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
public class TdAgentModelDescriptor {

    private String id;

    private String name;

    private boolean supportsMultimodal;

    private boolean supportsVideo;

    private String probeSource;
}
