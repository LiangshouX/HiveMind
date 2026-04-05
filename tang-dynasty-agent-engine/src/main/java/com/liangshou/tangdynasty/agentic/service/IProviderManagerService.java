package com.liangshou.tangdynasty.agentic.service;

import com.liangshou.tangdynasty.agentic.agents.provider.old.AbstractProvider;
import com.liangshou.tangdynasty.agentic.agents.provider.old.model.ProviderInfo;

/**
 * ProviderManagerService：供应商管理服务。
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
