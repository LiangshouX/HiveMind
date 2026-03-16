package com.tangdynasty.agent.engine.core;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Agent 抽象基类
 */
@Slf4j
public abstract class AbstractAgent implements Agent {
    
    protected String name;
    protected String title;
    
    public AbstractAgent(String name, String title) {
        this.name = name;
        this.title = title;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public void receiveTask(Task task) {
        log.info("[{}] Received task: {} - {}", title, task.getId(), task.getTitle());
    }
    
    @Override
    public ProgressReport reportProgress() {
        return ProgressReport.builder()
                .agentName(name)
                .state("UNKNOWN")
                .progressText("No progress yet")
                .build();
    }
    
    /**
     * 执行具体业务逻辑（由子类实现）
     */
    protected abstract TaskResult doExecute(Map<String, Object> context) throws Exception;
    
    @Override
    public TaskResult execute(Map<String, Object> context) throws Exception {
        try {
            log.info("[{}] Executing task...", title);
            TaskResult result = doExecute(context);
            log.info("[{}] Task execution completed", title);
            return result;
        } catch (Exception e) {
            log.error("[{}] Task execution failed: {}", title, e.getMessage(), e);
            return TaskResult.builder()
                    .success(false)
                    .errorMessage("Agent execution failed: " + e.getMessage())
                    .build();
        }
    }
}
