package com.liangshou.tangdynasty.agentic.agents.instance;

import io.agentscope.runtime.adapters.agentscope.AgentScopeAgentHandler;
import reactor.core.publisher.Flux;

/**
 * Agent 实例工厂接口。
 * <p>
 * 定义了基于 AgentScope Java 构建 Agent 的标准方式，支持流式输出和多 Agent Handoffs 交互。
 * </p>
 */
public interface AgentInstanceFactory {

    /**
     * 获取 Agent 名称。
     *
     * @return Agent 名称（如：ZDXL、VSUU、MFXX、UHUU）
     */
    String getName();

    /**
     * 获取 Agent 描述。
     *
     * @return Agent 描述信息
     */
    String getDescription();

    /**
     * 获取系统提示词。
     *
     * @return 系统提示词
     */
    String getSystemPrompt();

    /**
     * 流式查询接口。
     *
     * @param request  请求对象
     * @param messages 消息列表
     * @return 流式事件发布器
     */
    Flux<io.agentscope.core.agent.Event> streamQuery(
            io.agentscope.runtime.engine.schemas.AgentRequest request,
            Object messages);

    /**
     * 检查 Agent 是否健康。
     *
     * @return true 表示健康
     */
    boolean isHealthy();

    /**
     * 初始化 Agent。
     */
    void start();

    /**
     * 关闭 Agent，释放资源。
     *
     * @throws Exception 关闭异常
     */
    void close() throws Exception;
}
