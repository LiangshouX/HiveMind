package com.tangdynasty.agent.engine.core.impl;

import com.tangdynasty.agent.engine.core.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多 Agent 协作编排器实现
 */
@Slf4j
public class AgentOrchestratorImpl implements AgentOrchestrator {
    
    private final Map<String, Task> taskStore = new ConcurrentHashMap<>();
    private final Map<String, List<ProgressReport>> progressStore = new ConcurrentHashMap<>();
    
    private final ChengXiangAgent chengXiangAgent;
    private final ZhongshuAgent zhongshuAgent;
    
    public AgentOrchestratorImpl() {
        this.chengXiangAgent = new ChengXiangAgent();
        this.zhongshuAgent = new ZhongshuAgent();
    }
    
    @Override
    public void submitTask(Task task) {
        log.info("Submitting new task: {} - {}", task.getId(), task.getTitle());
        
        // 初始化任务状态
        task.setState(Task.TaskState.PENDING);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // 存储任务
        taskStore.put(task.getId(), task);
        
        // 开始处理
        processTask(task);
    }
    
    @Override
    public Task getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }
    
    @Override
    public void cancelTask(String taskId) {
        Task task = taskStore.get(taskId);
        if (task != null) {
            task.setState(Task.TaskState.CANCELLED);
            task.setUpdateTime(LocalDateTime.now());
            log.info("Task cancelled: {}", taskId);
        }
    }
    
    @Override
    public void resumeTask(String taskId) {
        Task task = taskStore.get(taskId);
        if (task != null && task.getState() == Task.TaskState.STOPPED) {
            // 恢复之前的状态
            task.setState(Task.TaskState.ZHONG_SHU); // 简化处理，恢复到中书省
            task.setUpdateTime(LocalDateTime.now());
            processTask(task);
        }
    }
    
    @Override
    public List<ProgressReport> getTaskProgress(String taskId) {
        return progressStore.getOrDefault(taskId, List.of());
    }
    
    @Override
    public void stopTask(String taskId, String reason) {
        Task task = taskStore.get(taskId);
        if (task != null) {
            task.setState(Task.TaskState.STOPPED);
            task.setUpdateTime(LocalDateTime.now());
            log.info("Task stopped: {}, reason: {}", taskId, reason);
        }
    }
    
    /**
     * 处理任务流转
     */
    private void processTask(Task task) {
        try {
            switch (task.getState()) {
                case PENDING, CHENG_XIANG -> handleChengXiang(task);
                case ZHONG_SHU, MEN_XIA -> handleZhongshu(task);
                case ASSIGNED, DOING -> handleShangshu(task);
                case DONE -> log.info("Task completed: {}", task.getId());
                default -> log.warn("Unknown task state: {}", task.getState());
            }
        } catch (Exception e) {
            log.error("Error processing task {}: {}", task.getId(), e.getMessage(), e);
            // 异常时停止任务
            stopTask(task.getId(), "执行异常：" + e.getMessage());
        }
    }
    
    /**
     * 处理丞相分拣
     */
    private void handleChengXiang(Task task) throws Exception {
        log.info("[编排器] 丞相处理任务");
        
        // 报告进度
        reportProgress(task, chengXiangAgent.reportProgress());
        
        // 执行
        task.setCurrentOrg("丞相");
        task.setState(Task.TaskState.CHENG_XIANG);
        
        TaskResult result = chengXiangAgent.execute(task.getContext());
        
        // 根据结果决定下一步
        if (result.getExtraData() != null) {
            String nextOrg = (String) result.getExtraData().get("nextOrg");
            if ("ZHONG_SHU".equals(nextOrg)) {
                task.setState(Task.TaskState.ZHONG_SHU);
                task.setUpdateTime(LocalDateTime.now());
                processTask(task); // 流转到下一个状态
            }
        }
    }
    
    /**
     * 处理中书省规划
     */
    private void handleZhongshu(Task task) throws Exception {
        log.info("[编排器] 中书省处理任务");
        
        // 报告进度
        reportProgress(task, zhongshuAgent.reportProgress());
        
        // 执行
        task.setCurrentOrg("中书省");
        task.setState(Task.TaskState.ZHONG_SHU);
        
        TaskResult result = zhongshuAgent.execute(task.getContext());
        
        // 流转到门下省审议
        task.setState(Task.TaskState.MEN_XIA);
        task.setUpdateTime(LocalDateTime.now());
        processTask(task);
    }
    
    /**
     * 处理尚书省执行（待实现）
     */
    private void handleShangshu(Task task) throws Exception {
        log.info("[编排器] 尚书省处理任务");
        // TODO: 实现尚书省和六部的逻辑
        task.setState(Task.TaskState.DONE);
        task.setUpdateTime(LocalDateTime.now());
    }
    
    /**
     * 报告进度
     */
    private void reportProgress(Task task, ProgressReport report) {
        progressStore.computeIfAbsent(task.getId(), k -> new java.util.ArrayList<>()).add(report);
        log.info("[{}] 进度汇报：{}", report.getAgentName(), report.getProgressText());
    }
}
