package com.tangdynasty.agent.engine.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 任务执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 结果内容
     */
    private String content;
    
    /**
     * 输出产物（文件路径、URL 等）
     */
    private String output;
    
    /**
     * 验收标准检查结果
     */
    private List<String> acResults;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 附加数据
     */
    private Map<String, Object> extraData;
}
