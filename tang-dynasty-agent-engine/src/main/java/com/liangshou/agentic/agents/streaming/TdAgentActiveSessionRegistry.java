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

    private static final Map<String, ReActAgent> activeAgents = new ConcurrentHashMap<>();

    /**
     * 执行 register 操作。
     *
     * @param key   会话键
     * @param agent 参数
     */
    public void register(String key, ReActAgent agent) {
        log.info("[会话注册] 注册活动会话 - key: {}, agentName: {}", key, agent.getName());
        activeAgents.put(key, agent);
        log.debug("[会话注册] 会话已注册，当前活动会话数: {}", activeAgents.size());
    }

    /**
     * 执行 unregister 操作。
     *
     * @param key 会话键
     */
    public void unregister(String key) {
        log.info("[会话注销] 注销活动会话 - key: {}", key);
        ReActAgent removed = activeAgents.remove(key);
        if (removed != null) {
            log.debug("[会话注销] 会话已成功注销，当前活动会话数: {}", activeAgents.size());
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
        ReActAgent agent = activeAgents.get(key);
        if (agent == null) {
            log.warn("[会话中断] 未找到活动会话，可能已被注销 - key: {}", key);
            return false;
        }
        try {
            log.info("[会话中断] 找到活动会话，调用 interrupt() 方法 - agentName: {}", agent.getName());
            agent.interrupt();
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
        return activeAgents.size();
    }
}
