package com.tangdynasty.agent.engine.graph.base;

import com.alibaba.cloud.ai.graph.GraphState;
import com.alibaba.cloud.ai.graph.NodeAction;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 部门执行节点基类
 * 
 * 提供统一的错误处理、重试机制和日志记录
 */
@Slf4j
public abstract class AbstractDepartmentNode implements NodeAction {
    
    protected final String departmentName;
    
    public AbstractDepartmentNode(String departmentName) {
        this.departmentName = departmentName;
    }
    
    @Override
    public GraphState execute(GraphState state) throws Exception {
        log.info("[{}] 开始执行任务...", departmentName);
        
        int retryCount = getRetryCount(state);
        int maxRetries = 3; // 最大重试次数
        
        try {
            // 执行具体的部门逻辑
            DepartmentResult result = doExecute(state);
            
            if (result.isSuccess()) {
                log.info("[{}] 任务执行成功", departmentName);
                state.updateState(Map.of(
                    departmentName + "_status", "COMPLETED",
                    departmentName + "_result", result.getResult(),
                    "nodeStatus", "SUCCESS"
                ));
            } else {
                throw new RuntimeException("任务执行失败：" + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("[{}] 任务执行失败 (第{}次尝试)", departmentName, retryCount + 1, e);
            
            if (retryCount < maxRetries) {
                // 重试
                log.info("[{}] 将在 5 秒后重试...", departmentName);
                TimeUnit.SECONDS.sleep(5);
                
                state.updateState(Map.of(
                    "retryCount", retryCount + 1,
                    "lastError", e.getMessage()
                ));
                
                // 递归调用自己进行重试
                return execute(state);
            } else {
                // 超过最大重试次数，标记为失败
                log.error("[{}] 已达到最大重试次数，任务失败", departmentName);
                state.updateState(Map.of(
                    departmentName + "_status", "FAILED",
                    "errorMessage", e.getMessage(),
                    "nodeStatus", "ERROR"
                ));
            }
        }
        
        return state;
    }
    
    /**
     * 执行具体的部门逻辑
     * 
     * @param state 当前状态
     * @return 执行结果
     */
    protected abstract DepartmentResult doExecute(GraphState state) throws Exception;
    
    /**
     * 获取重试次数
     */
    protected int getRetryCount(GraphState state) {
        return state.value("retryCount")
                .map(v -> Integer.parseInt(v.toString()))
                .orElse(0);
    }
    
    /**
     * 部门执行结果
     */
    public static class DepartmentResult {
        private final boolean success;
        private final String result;
        private final String errorMessage;
        
        public DepartmentResult(boolean success, String result, String errorMessage) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
        }
        
        public static DepartmentResult success(String result) {
            return new DepartmentResult(true, result, null);
        }
        
        public static DepartmentResult failure(String errorMessage) {
            return new DepartmentResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getResult() {
            return result;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
