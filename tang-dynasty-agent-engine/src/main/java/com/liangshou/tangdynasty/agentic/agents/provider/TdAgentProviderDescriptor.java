package com.liangshou.tangdynasty.agentic.agents.provider;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商描述符 - 描述 LLM 供应商的完整配置信息。
 *
 * <p>该对象包含以下核心配置：</p>
 * <ul>
 *     <li><b>基本信息</b>：id、name、providerType（DASHSCOPE/OPENAI）</li>
 *     <li><b>连接配置</b>：baseUrl、apiKey、endpointPath、apiKeyPrefix</li>
 *     <li><b>模型配置</b>：chatModel（默认模型）、models（可用模型列表）</li>
 *     <li><b>格式化器</b>：formatter（openai/deepseek），决定消息格式转换方式</li>
 *     <li><b>行为标志</b>：freezeUrl（是否锁定 URL）、isCustom（是否自定义供应商）、supportModelDiscovery（是否支持模型发现）</li>
 *     <li><b>生成参数</b>：generateKwargs，供应商特定的额外请求参数</li>
 * </ul>
 *
 * <p>该描述符由 {@link TdAgentProviderRegistry} 从 JSON 配置文件加载，作为创建聊天模型的模板。</p>
 *
 * @author LiangshouX
 */
@Getter
@Setter
public class TdAgentProviderDescriptor {

    private String id;

    private String name;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private String endpointPath;

    private String chatModel;

    private String formatter;

    private List<TdAgentModelDescriptor> models = new ArrayList<>();

    private String apiKeyPrefix;

    private boolean freezeUrl;

    private boolean isCustom;

    private boolean supportModelDiscovery;

    private boolean supportConnectionCheck = true;

    private Map<String, Object> generateKwargs = new LinkedHashMap<>();
}

