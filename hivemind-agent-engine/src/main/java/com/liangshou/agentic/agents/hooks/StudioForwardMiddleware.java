package com.liangshou.agentic.agents.hooks;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.studio.StudioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import reactor.core.publisher.Flux;

/**
 * Studio 消息转发中间件 — 替代已废弃的 {@code StudioMessageHook}。
 *
 * <p>在 Agent 事件流中拦截 {@link AgentResultEvent}，将最终回复消息转发到
 * AgentScope Studio 用于可视化追踪。</p>
 *
 * <p>转发失败不会中断 Agent 执行，仅记录错误日志。</p>
 */
public class StudioForwardMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(StudioForwardMiddleware.class);

    private final StudioClient studioClient;

    public StudioForwardMiddleware(StudioClient studioClient) {
        this.studioClient = studioClient;
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext ctx,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        return next.apply(input)
                .doOnNext(event -> {
                    if (event instanceof AgentResultEvent resultEvent) {
                        forwardToStudio(resultEvent.getResult());
                    }
                });
    }

    private void forwardToStudio(Msg msg) {
        if (studioClient == null) {
            log.warn("[StudioForward] StudioClient 为 null，跳过消息转发");
            return;
        }
        if (msg == null) {
            return;
        }
        studioClient.pushMessage(msg)
                .doOnError(ex -> log.error("[StudioForward] 转发消息到 Studio 失败", ex))
                .onErrorResume(ex -> reactor.core.publisher.Mono.empty())
                .subscribe();
    }
}
