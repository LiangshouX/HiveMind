package com.liangshou.agentic.common.util;

import com.liangshou.agentic.application.dto.ToolConfigDTO;
import com.liangshou.agentic.application.IToolConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具配置提供者 - 提供带缓存的工具配置查询功能。
 *
 * <p>该类为 ToolGuardEngine 和 TdAgentToolkitFactory 提供高效的工具配置查询：</p>
 * <ul>
 *     <li>使用本地缓存减少 MongoDB 查询频率</li>
 *     <li>缓存 TTL 5 分钟自动过期</li>
 *     <li>支持手动刷新缓存（配置更新时）</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
@Slf4j
public class ToolConfigProvider {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 分钟

    private final IToolConfigService toolConfigService;
    private final Map<String, CacheEntry<ToolConfigDTO>> configCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<ToolConfigDTO>>> userCache = new ConcurrentHashMap<>();

    public ToolConfigProvider(IToolConfigService toolConfigService) {
        this.toolConfigService = toolConfigService;
    }

    /**
     * 获取单个工具的有效配置。
     *
     * @param toolName 工具名称
     * @param userId   用户唯一标识
     * @return 工具配置
     */
    public ToolConfigDTO getConfig(String toolName, String userId) {
        String cacheKey = userId + ":" + toolName;
        CacheEntry<ToolConfigDTO> entry = configCache.get(cacheKey);

        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }

        // 缓存未命中，查询服务
        ToolConfigDTO config = toolConfigService.getEffectiveConfig(toolName, userId);
        configCache.put(cacheKey, new CacheEntry<>(config, CACHE_TTL_MS));
        return config;
    }

    /**
     * 获取用户的所有工具配置。
     *
     * @param userId 用户唯一标识
     * @return 工具配置列表
     */
    public List<ToolConfigDTO> getAllConfigs(String userId) {
        CacheEntry<List<ToolConfigDTO>> entry = userCache.get(userId);

        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }

        // 缓存未命中，查询服务
        List<ToolConfigDTO> configs = toolConfigService.listByUserId(userId);
        userCache.put(userId, new CacheEntry<>(configs, CACHE_TTL_MS));

        // 同时填充单个工具缓存
        for (ToolConfigDTO config : configs) {
            String cacheKey = userId + ":" + config.getToolName();
            configCache.put(cacheKey, new CacheEntry<>(config, CACHE_TTL_MS));
        }

        return configs;
    }

    /**
     * 刷新用户的工具配置缓存。
     *
     * <p>在配置更新后调用，确保后续查询获取最新配置。</p>
     *
     * @param userId 用户唯一标识
     */
    public void refreshCache(String userId) {
        // 清除该用户的所有缓存
        userCache.remove(userId);
        configCache.keySet().removeIf(key -> key.startsWith(userId + ":"));
        log.debug("[ToolConfigProvider] 刷新用户 {} 的配置缓存", userId);
    }

    /**
     * 缓存条目。
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long expiresAt;

        CacheEntry(T data, long ttlMs) {
            this.data = data;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        T getData() {
            return data;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
