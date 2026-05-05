package com.liangshou.tangdynasty.agentic.infrastructure.mysql.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Token 消耗统计表 - Agent 引擎专用
 * 
 * <p>用于记录 Agent 对话中的 LLM Token 使用量，支持按用户、模型、供应商等维度统计。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("td_token_usage")
public class TokenUsagePO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 消息ID（AgentScope Msg ID）
     */
    @TableField("message_id")
    private String messageId;

    /**
     * 模型供应商（dashscope/openai）
     */
    @TableField("model_provider")
    private String modelProvider;

    /**
     * 模型名称（如 qwen-max、gpt-4o）
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 输入Token数
     */
    @TableField("input_tokens")
    private Integer inputTokens;

    /**
     * 输出Token数
     */
    @TableField("output_tokens")
    private Integer outputTokens;

    /**
     * 总Token数
     */
    @TableField("total_tokens")
    private Integer totalTokens;

    /**
     * 缓存命中Token数（可选）
     */
    @TableField("cached_tokens")
    private Integer cachedTokens;

    /**
     * 使用时间
     */
    @TableField("usage_time")
    private LocalDateTime usageTime;

    /**
     * 创建时间（自动填充）
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
