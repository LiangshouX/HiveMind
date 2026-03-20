package com.tangdynasty.agent.engine.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * AI 模型服务（带缓存）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIModelService {
    
    private final DashScopeChatModel chatModel;
    
    /**
     * 调用 AI 模型（带缓存）
     * 
     * @param promptKey 提示词的唯一标识（用于缓存 key）
     * @param prompt 实际提示词
     * @return AI 响应结果
     */
    @Cacheable(value = "ai-responses", key = "#promptKey")
    public String callWithCache(String promptKey, String prompt) {
        log.debug("AI 模型调用 (cache key: {})", promptKey);
        
        try {
            var messages = new java.util.ArrayList<org.springframework.ai.chat.messages.Message>();
            messages.add(new org.springframework.ai.chat.messages.SystemMessage("你是一个专业的助手。"));
            messages.add(new org.springframework.ai.chat.messages.UserMessage(prompt));
            
            String response = chatModel.call(
                new org.springframework.ai.chat.prompt.Prompt(messages)
            ).getResult().getOutput().getText();
            
            log.debug("AI 响应成功，长度：{}", response.length());
            return response;
            
        } catch (Exception e) {
            log.error("AI 模型调用失败：{}", promptKey, e);
            throw new RuntimeException("AI 模型调用失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 清除缓存
     */
    public void evictCache(String promptKey) {
        log.info("清除缓存：{}", promptKey);
        // Spring Cache 会自动处理
    }
}
