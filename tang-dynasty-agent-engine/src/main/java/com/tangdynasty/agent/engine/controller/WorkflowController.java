package com.tangdynasty.agent.engine.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.tangdynasty.agent.engine.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流执行控制器
 */
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {
    
    private final WorkflowService workflowService;
    
    /**
     * 执行工作流（同步）
     */
    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody Map<String, String> request) throws Exception {
        String content = request.get("content");
        log.info("接收到工作流请求：{}", content);
        
        OverAllState result = workflowService.execute(content);
        
        return Map.of(
            "success", true,
            "data", extractResult(result),
            "message", "执行成功"
        );
    }
    
    /**
     * 异步执行工作流
     */
    @PostMapping("/execute/async")
    public CompletableFuture<Map<String, Object>> executeAsync(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        log.info("接收到异步工作流请求：{}", content);
        
        return workflowService.executeAsync(content)
                .thenApply(result -> Map.of(
                    "success", true,
                    "data", extractResult(result),
                    "message", "异步执行成功"
                ));
    }
    
    /**
     * 流式输出（SSE）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String content) {
        log.info("接收到流式请求：{}", content);
        
        // TODO: 实现流式输出逻辑
        return Flux.just("Event 1", "Event 2", "Event 3");
    }
    
    /**
     * 提取结果数据
     */
    private Map<String, Object> extractResult(OverAllState state) {
        return Map.of(
            "response", state.value("response").orElse(null),
            "plan", state.value("plan").orElse(null),
            "todos", state.value("todos").orElse(null),
            "status", state.value("nodeStatus").orElse("UNKNOWN")
        );
    }
}
