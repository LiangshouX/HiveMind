package com.liangshou.tangdynasty.agentic.service;

import com.liangshou.tangdynasty.agentic.agents.provider.old.AbstractProvider;
import com.liangshou.tangdynasty.agentic.agents.provider.old.model.ProviderInfo;

/**
 * 供应商管理服务接口 - 提供模型供应商配置的查询和管理功能。
 *
 * <p>该接口定义以下操作：</p>
 * <ul>
 *     <li>{@link #getProviderInfo(String)} - 获取指定供应商的配置信息（脱敏后的快照）</li>
 *     <li>{@link #getProvider(String)} - 获取指定供应商的抽象提供者实例，用于执行模型调用</li>
 * </ul>
 *
 * <p>注意：该接口位于旧版实现中，建议优先使用新的 {@link com.liangshou.tangdynasty.agentic.agents.provider.TdAgentProviderRegistry}。</p>
 *
 * @author LiangshouX
 */
public interface IProviderManagerService {

    /**
     * 获取指定供应商配置。
     *
     * @param providerId 供应商标识
     * @return 供应商信息
     */
    ProviderInfo getProviderInfo(String providerId);

    /**
     * 获取指定供应商配置。
     *
     * @param providerId 供应商标识
     * @return 供应商信息
     */
    AbstractProvider getProvider(String providerId);
}
