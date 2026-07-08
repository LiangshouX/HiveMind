package com.liangshou.agentic.infrastructure.provider.query;

import java.util.List;

/**
 * 供应商配置查询接口 - 供 Agent Engine 跨模块调用。
 *
 * <p>该接口定义在 agent-engine 模块中（消费方），
 * 由 backend 模块提供实现。通过 Spring 依赖注入在 launcher 中聚合。</p>
 *
 * <p>性能约束：</p>
 * <ul>
 *     <li>{@link #getByProviderId} 使用 {@code (user_id, model_provider_id)} 唯一索引</li>
 *     <li>{@link #getActivated} 使用 {@code (user_id, is_provider_activated)} 组合索引</li>
 *     <li>单次查询延迟目标 < 5ms</li>
 * </ul>
 *
 * @author LiangshouX
 */
public interface IProviderQueryService {

    /**
     * 查询用户所有供应商配置。
     *
     * @param userId 用户 ID
     * @return 供应商配置列表，无记录时返回空列表
     */
    List<ProviderQueryResult> listByUserId(String userId);

    /**
     * 查询用户指定供应商的配置。
     *
     * @param userId       用户 ID
     * @param providerId   供应商 ID（model_provider_id）
     * @return 供应商配置，不存在时返回 null
     */
    ProviderQueryResult getByProviderId(String userId, String providerId);

    /**
     * 查询用户当前激活的默认供应商配置。
     *
     * @param userId 用户 ID
     * @return 激活的供应商配置，无激活供应商时返回 null
     */
    ProviderQueryResult getActivated(String userId);

    /**
     * 获取指定供应商解密后的 API Key。
     *
     * @param userId 用户 ID
     * @param id     供应商记录主键 ID
     * @return 解密后的 API Key，不存在时返回 null
     */
    String getDecryptedApiKey(String userId, Long id);
}
