package com.tangdynasty.agent.engine.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.tangdynasty.agent.engine.graph.TangDynastyWorkflowBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 三省六部工作流服务
 * 
 * 基于Spring AI Alibaba 官方 Graph 框架
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {
    
    private final TangDynastyWorkflowBuilder workflowBuilder;
    private CompiledGraph compiledGraph;
    
    /**
     * 初始化工作流图
     */
    public void initGraph() throws Exception {
        log.info("Initializing Tang Dynasty workflow graph...");
        this.compiledGraph = workflowBuilder.buildWorkflow();
        log.info("Workflow graph initialized successfully");
    }
    
    /**
     * 执行工作流
     * 
     * @param content 用户输入内容
     * @return 执行结果
     */
    public OverAllState execute(String content) throws Exception {
        if (compiledGraph == null) {
            throw new IllegalStateException("Workflow graph not initialized");
        }
        
        log.info("Executing workflow with content: {}", content);
        
        // 创建初始状态
        OverAllState initialState = new OverAllState();
        initialState.updateState(Map.of(
            "content", content,
            "startTime", System.currentTimeMillis()
        ));
        
        // 执行图
        OverAllState result = compiledGraph.invoke(initialState);
        
        log.info("Workflow execution completed");
        return result;
    }
    
    /**
     * 异步执行工作流
     */
    public java.util.concurrent.CompletableFuture<OverAllState> executeAsync(String content) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return execute(content);
            } catch (Exception e) {
                throw new RuntimeException("Workflow execution failed", e);
            }
        });
    }
}
