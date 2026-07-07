package com.liangshou.agentic.agents.streaming;

import io.agentscope.core.ReActAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活动会话注册表，用于跟踪和管理正在执行的流式 Agent 会话。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>注册活动会话，将用户 ID + 会话 ID 映射到对应的 ReActAgent 实例</li>
 *   <li>注销已完成或中断的会话</li>
 *   <li>中断指定会话的执行，调用 Agent 的 interrupt() 方法</li>
 * </ul>
 * </p>
 * <p>
 * 该组件使用 ConcurrentHashMap 实现线程安全的会话管理，
 * 支持并发访问多个用户的流式会话。会话键格式为 "userId:sessionId"。
 * </p>
 *
 * @author LiangshouX
 */
@Component
public class TdAgentActiveSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(TdAgentActiveSessionRegistry.class);

    private static final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    /**
     * 活动会话信息，跟踪会话使用的 Agent 及其模型配置。
     *
     * @param agent      活动会话的 ReActAgent 实例
     * @param providerId 该会话使用的供应商 ID
     * @param modelId    该会话使用的模型 ID
     */
    private record SessionInfo(ReActAgent agent, String providerId, String modelId) {}

    /**
     * 执行 register 操作。
     *
     * @param key   会话键
     * @param agent 参数
     */
    public void register(String key, ReActAgent agent) {
        register(key, agent, null, null);
    }

    /**
     * 执行 register 操作（包含模型信息）。
     *
     * @param key        会话键
     * @param agent      参数
     * @param providerId 供应商 ID
     * @param modelId    模型 ID
     */
    public void register(String key, ReActAgent agent, String providerId, String modelId) {
        log.info("[会话注册] 注册活动会话 - key: {}, agentName: {}, providerId: {}, modelId: {}",
                key, agent.getName(), providerId, modelId);
        activeSessions.put(key, new SessionInfo(agent, providerId, modelId));
        log.debug("[会话注册] 会话已注册，当前活动会话数: {}", activeSessions.size());
    }

    /**
     * 执行 unregister 操作。
     *
     * @param key 会话键
     */
    public void unregister(String key) {
        log.info("[会话注销] 注销活动会话 - key: {}", key);
        SessionInfo removed = activeSessions.remove(key);
        if (removed != null) {
            log.debug("[会话注销] 会话已成功注销，当前活动会话数: {}", activeSessions.size());
        } else {
            log.warn("[会话注销] 尝试注销不存在的会话 - key: {}", key);
        }
    }

    /**
     * 中断当前执行。
     *
     * @param key 会话键
     * @return 返回结果
     */
    public boolean interrupt(String key) {
        log.info("[会话中断] 尝试中断会话 - key: {}", key);
        SessionInfo info = activeSessions.get(key);
        if (info == null) {
            log.warn("[会话中断] 未找到活动会话，可能已被注销 - key: {}", key);
            return false;
        }
        try {
            log.info("[会话中断] 找到活动会话，调用 interrupt() 方法 - agentName: {}", info.agent().getName());
            info.agent().interrupt();
            log.info("[会话中断] 中断信号已发送 - key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("[会话中断] 发送中断信号失败 - key: {}, error: {}", key, e.getMessage(), e);
            // 即使中断失败，也尝试注销该会话
            unregister(key);
            return false;
        }
    }

    /**
     * 获取当前活动会话数量。
     *
     * @return 活动会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
