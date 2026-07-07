package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Token消耗统计表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_token_usage")
public class SysTokenUsagePO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**

     * 主键ID

     */

    @TableId(type = IdType.AUTO)
    private Long id;

    /**

     * 任务所属的用户ID

     */

    @TableField("user_id")

    private Long userId;

    /**

     * 模型供应商

     */

    @TableField("model_provider_id")

    private String modelProviderId;

    /**

     * 请求的模型ID

     */

    @TableField("model_id")

    private String modelId;

    /**

     * 调用的官员

     */

    @TableField("official_id")

    private Long officialId;

    /**

     * 输入Token数

     */

    @TableField("prompt_tokens")

    private Integer promptTokens;

    /**

     * 输出Token数

     */

    @TableField("completion_tokens")

    private Integer completionTokens;

    /**

     * 总Token数

     */

    @TableField("total_tokens")

    private Integer totalTokens;

    /**

     * 命中缓存的Token数量

     */

    @TableField("cached_prompt_tokens")

    private Integer cachedPromptTokens;

    /**

     * 记录时间

     */

    @TableField(fill = FieldFill.INSERT, value = "record_time")

    private LocalDateTime recordTime;

}
