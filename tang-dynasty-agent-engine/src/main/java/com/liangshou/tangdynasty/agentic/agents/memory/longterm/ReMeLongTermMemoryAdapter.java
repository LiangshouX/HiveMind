package com.liangshou.tangdynasty.agentic.agents.memory.longterm;

import io.agentscope.core.memory.reme.ReMeLongTermMemory;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ReMeLongTermMemoryAdapter：基于 ReMe + Chroma 长期记忆后端适配器
 * <p>
 * 参考文档如下：
 * </p>
 * <ul>
 *     <li><a href="https://gitee.com/chenfei6095/ReMe">ReMe</a></li>
 *     <li><a href="https://docs.trychroma.com/docs/overview/introduction">Chroma</a></li>
 *     <li><a href="https://java.agentscope.io/zh/task/memory.html#remelongtermmemory">AgentScope Java Long Term Memory Docs</a></li>
 *     <li><a href="https://github.com/agentscope-ai/agentscope-java/blob/main/agentscope-examples/advanced/src/main/java/io/agentscope/examples/advanced/ReMeExample.java">AgentScope Java Long Term Memory Example</a></li>
 * </ul>
 */
@Getter
@Component
@SuppressWarnings("unused")
public class ReMeLongTermMemoryAdapter {

    /**
     * 从 application.yaml 的 agentic.memory.reme.baseUrl 属性获取 ReMe API base URL
     * <br/>
     * 优先级：环境变量 > YAML 配置 > 默认值
     * <br/>
     * -- GETTER --
     * <br/>
     * 获取 ReMe API base URL
     */
    @Value("${agentic.memory.reme.baseUrl:#{environment['REME_API_BASE_URL'] ?: 'http://localhost:8002'}}")
    private String baseUrl;

    /**
     * 创建 ReMeLongTermMemory 对象（实例方法）
     *
     * @param userId 用户 ID
     * @return ReMeLongTermMemory 实例
     */
    public ReMeLongTermMemory create(String userId) {
        return ReMeLongTermMemory.builder()
                .userId(userId)
                .apiBaseUrl(this.baseUrl)
                .build();
    }

    /**
     * 创建 ReMeLongTermMemory 对象（静态工厂方法，适用于无 Spring 容器场景）
     *
     * @param userId  用户 ID
     * @param baseUrl ReMe API 基础地址
     * @return ReMeLongTermMemory 实例
     */
    public static ReMeLongTermMemory create(String userId, String baseUrl) {
        return ReMeLongTermMemory.builder()
                .userId(userId)
                .apiBaseUrl(baseUrl)
                .build();
    }
}
