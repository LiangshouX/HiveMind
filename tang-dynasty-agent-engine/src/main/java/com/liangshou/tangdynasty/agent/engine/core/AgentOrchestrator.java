package com.tangdynasty.agent.engine.core;

import java.util.List;

/**
 * 多 Agent 协作编排器
 * 
 * 负责管理三省六部的业务流转
 */
public interface AgentOrchestrator {
    
    /**
     * 接收新任务/旨意
     */
    void submitTask(Task task);
    
    /**
     * 获取任务状态
     */
    Task getTaskStatus(String taskId);
    
    /**
     * 取消任务
     */
    void cancelTask(String taskId);
    
    /**
     * 恢复任务
     */
    void resumeTask(String taskId);
    
    /**
     * 获取任务进度报告
     */
    List<ProgressReport> getTaskProgress(String taskId);
    
    /**
     * 停止任务
     */
    void stopTask(String taskId, String reason);
}
