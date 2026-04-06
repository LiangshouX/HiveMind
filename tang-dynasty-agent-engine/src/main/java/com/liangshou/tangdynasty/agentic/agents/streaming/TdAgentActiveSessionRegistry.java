package com.liangshou.tangdynasty.agentic.agents.streaming;

import io.agentscope.core.ReActAgent;
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

    private final Map<String, ReActAgent> activeAgents = new ConcurrentHashMap<>();

    /**
     * 执行 register 操作。
     * @param key 会话键
     * @param agent 参数
     */
    public void register(String key, ReActAgent agent) {
        activeAgents.put(key, agent);
    }

    /**
     * 执行 unregister 操作。
     * @param key 会话键
     */
    public void unregister(String key) {
        activeAgents.remove(key);
    }

    /**
     * 中断当前执行。
     * @param key 会话键
     * @return 返回结果
     */
    public boolean interrupt(String key) {
        ReActAgent agent = activeAgents.get(key);
        if (agent == null) {
            return false;
        }
        agent.interrupt();
        return true;
    }
}
