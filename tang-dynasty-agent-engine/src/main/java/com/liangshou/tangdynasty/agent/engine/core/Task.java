package com.tangdynasty.agent.engine.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务/旨意定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    /**
     * 任务 ID（如：JJC-20260317-abcd）
     */
    private String id;
    
    /**
     * 任务标题
     */
    private String title;
    
    /**
     * 任务描述/内容
     */
    private String description;
    
    /**
     * 任务类型（闲聊、旨意、任务等）
     */
    private TaskType type;
    
    /**
     * 优先级
     */
    private Priority priority;
    
    /**
     * 当前负责部门
     */
    private String currentOrg;
    
    /**
     * 当前状态
     */
    private TaskState state;
    
    /**
     * 上下文参数
     */
    private Map<String, Object> context;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 任务类型枚举
     */
    public enum TaskType {
        CHAT,      // 闲聊
        EDICT,     // 旨意
        TASK       // 任务
    }
    
    /**
     * 优先级枚举
     */
    public enum Priority {
        CRITICAL,  // 紧急
        HIGH,      // 高
        NORMAL,    // 普通
        LOW        // 低
    }
    
    /**
     * 任务状态枚举（对应三省六部流转）
     */
    public enum TaskState {
        PENDING,       // 待处理
        CHENG_XIANG,   // 丞相分拣
        ZHONG_SHU,     // 中书省规划
        MEN_XIA,       // 门下省审议
        ASSIGNED,      // 已准奏
        DOING,         // 尚书省派发执行
        REVIEW,        // 复核
        DONE,          // 完成
        STOPPED,       // 停止
        CANCELLED      // 取消
    }
}
