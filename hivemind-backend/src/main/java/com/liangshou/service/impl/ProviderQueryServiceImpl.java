package com.liangshou.service.impl;

import com.liangshou.agentic.infrastructure.provider.query.IProviderQueryService;
import com.liangshou.agentic.infrastructure.provider.query.ProviderQueryResult;
import com.liangshou.infrastructure.datasource.po.SysModelsPO;
import com.liangshou.infrastructure.datasource.support.ISysModelsSupport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商配置查询服务实现 - 供 Agent Engine 跨模块调用。
 *
 * <p>实现 agent-engine 模块定义的 {@link IProviderQueryService} 接口，
 * 通过 {@link ISysModelsSupport} 直接查询 {@code sys_models} 表。</p>
 *
 * <p>性能保证：</p>
 * <ul>
 *     <li>{@link #getByProviderId} 使用 {@code (user_id, model_provider_id)} 唯一索引</li>
 *     <li>{@link #getActivated} 使用 {@code (user_id, is_provider_activated)} 组合索引</li>
 *     <li>API Key 解密委托给 {@link SysModelsServiceImpl}</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Service
@RequiredArgsConstructor
public class ProviderQueryServiceImpl implements IProviderQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProviderQueryServiceImpl.class);

    private final ISysModelsSupport sysModelsSupport;
    private final SysModelsServiceImpl sysModelsService;

    @Override
    public List<ProviderQueryResult> listByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }
        List<SysModelsPO> poList = sysModelsSupport.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .list();
        return poList.stream().map(this::toResult).collect(Collectors.toList());
    }

    @Override
    public ProviderQueryResult getByProviderId(String userId, String providerId) {
        if (userId == null || userId.isBlank() || providerId == null || providerId.isBlank()) {
            return null;
        }
        SysModelsPO po = sysModelsSupport.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .eq(SysModelsPO::getModelProviderId, providerId)
                .one();
        return po == null ? null : toResult(po);
    }

    @Override
    public ProviderQueryResult getActivated(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        SysModelsPO po = sysModelsSupport.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .eq(SysModelsPO::getIsProviderActivated, 1)
                .orderByDesc(SysModelsPO::getUpdateTime)
                .last("LIMIT 1")
                .one();
        return po == null ? null : toResult(po);
    }

    @Override
    public String getDecryptedApiKey(String userId, Long id) {
        return sysModelsService.getDecryptedApiKey(userId, id);
    }

    private ProviderQueryResult toResult(SysModelsPO po) {
        ProviderQueryResult result = new ProviderQueryResult();
        result.setId(po.getId());
        result.setUserId(po.getUserId());
        result.setModelProviderId(po.getModelProviderId());
        result.setProviderName(po.getProviderName());
        result.setModelProviderType(po.getModelProviderType());
        result.setActivated(po.getIsProviderActivated() != null && po.getIsProviderActivated() == 1);
        result.setSystemBuiltIn(po.getIsSystemBuiltIn() != null && po.getIsSystemBuiltIn() == 1);
        result.setBaseUrl(po.getBaseUrl());
        result.setEncryptedApiKey(po.getApiKey());
        result.setModelsJson(po.getModelsJson());
        result.setSelectedModelId(po.getModelId());
        result.setSelectedModelName(po.getModelName());
        return result;
    }
}
