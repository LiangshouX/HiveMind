package com.tangdynasty.agent.engine.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 进度报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReport {
    
    /**
     * 汇报时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 汇报 Agent
     */
    private String agentName;
    
    /**
     * 当前状态
     */
    private String state;
    
    /**
     * 进度文本
     */
    private String progressText;
    
    /**
     * 待办任务列表
     */
    private List<TodoItem> todos;
    
    /**
     * Token 消耗
     */
    private Integer tokens;
    
    /**
     * 成本
     */
    private Double cost;
    
    /**
     * 耗时（秒）
     */
    private Long elapsed;
    
    /**
     * 待办项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoItem {
        private String id;
        private String title;
        private String status; // not-started, in-progress, completed
    }
}
