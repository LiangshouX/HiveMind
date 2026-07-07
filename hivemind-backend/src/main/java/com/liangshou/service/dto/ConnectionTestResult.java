package com.liangshou.service.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 连接测试结果 DTO
 */
@Data
public class ConnectionTestResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 是否可达 */
    private boolean reachable;

    /** 响应延迟（毫秒） */
    private long latencyMs;

    /** 发现的模型列表 */
    private List<ModelInfo> discoveredModels;

    /** 错误消息（仅在不可达时填充） */
    private String errorMessage;

    /**
     * 发现的模型信息
     */
    @Data
    public static class ModelInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 模型 ID */
        private String id;

        /** 模型显示名称 */
        private String name;
    }
}
