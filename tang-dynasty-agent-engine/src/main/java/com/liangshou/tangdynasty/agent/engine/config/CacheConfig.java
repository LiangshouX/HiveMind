package com.tangdynasty.agent.engine.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 
 * 使用 Caffeine 实现高性能本地缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 配置缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 注册不同的缓存区域
        cacheManager.registerCustomCache("workflow-results", createWorkflowResultsCache());
        cacheManager.registerCustomCache("ai-responses", createAIResponsesCache());
        cacheManager.registerCustomCache("department-tasks", createDepartmentTasksCache());
        
        return cacheManager;
    }
    
    /**
     * 工作流结果缓存
     * 
     * 策略：
     - 最大容量：1000 个
     - 过期时间：10 分钟（基于写入时间）
     - 适用场景：存储已执行的工作流结果，避免重复计算
     */
    private Cache<String, Object> createWorkflowResultsCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    /**
     * AI 响应缓存
     * 
     * 策略：
     - 最大容量：5000 个
     - 过期时间：30 分钟（基于访问时间）
     - 适用场景：缓存相同的 AI 模型调用结果
     */
    private Cache<String, String> createAIResponsesCache() {
        return Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
    
    /**
     * 部门任务缓存
     * 
     * 策略：
     - 最大容量：2000 个
     - 过期时间：5 分钟（基于写入时间）
     - 适用场景：缓存部门的待办任务列表
     */
    private Cache<String, Object> createDepartmentTasksCache() {
        return Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}
