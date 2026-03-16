package com.tangdynasty.agent.engine.core;

import java.util.Map;

/**
 * Agent 接口 - 三省六部制度中的官员
 */
public interface Agent {
    
    /**
     * 获取官员名称（如：丞相、中书令等）
     */
    String getName();
    
    /**
     * 获取官职（如：丞相、中书省、礼部等）
     */
    String getTitle();
    
    /**
     * 接收旨意/任务
     */
    void receiveTask(Task task);
    
    /**
     * 执行任务
     */
    TaskResult execute(Map<String, Object> context) throws Exception;
    
    /**
     * 汇报进度
     */
    ProgressReport reportProgress();
    
    /**
     * 是否可以处理该任务
     */
    boolean canHandle(Task task);
}
