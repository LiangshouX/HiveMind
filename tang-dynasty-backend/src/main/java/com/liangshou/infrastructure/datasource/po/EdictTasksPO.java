package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务/旨意实例主表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("edict_tasks")
public class EdictTasksPO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID, 如 JJC-20260228-E2E
     */

    @TableId(type = IdType.INPUT)
    private String taskId;

    /**
     * 任务对应的session_id
     */

    @TableField("session_id")

    private String sessionId;

    /**
     * 登录的用户ID
     */

    @TableField("user_id")

    private Long userId;

    /**
     * 任务标题
     */

    @TableField("title")

    private String title;

    /**
     * 当前负责官员ID
     */

    @TableField("official_id")

    private Long officialId;

    /**
     * 当前状态: PENDING/ZDXL/VSUU/MFXX/ASSIGNED/DOING/PREVIEW/DONE/FINISH
     */

    @TableField("state")

    private String state;

    /**
     * 优先级: critical/high/normal/low
     */

    @TableField("priority")

    private String priority;

    /**
     * 阻滞原因
     */

    @TableField("block_reason")

    private String blockReason;

    /**
     * 要求修改的轮数
     */

    @TableField("review_round")

    private Integer reviewRound;

    /**
     * 被中断前的状态
     */

    @TableField("prev_state")

    private String prevState;

    /**
     * 最终产出结果
     */

    @TableField("output_result")

    private String outputResult;

    /**
     * 验收标准
     */

    @TableField("ac_criteria")

    private String acCriteria;

    /**
     * 是否归档: 0-否, 1-是
     */

    @TableField("archived")

    private Integer archived;

    /**
     * 归档时间
     */

    @TableField("archived_at")

    private LocalDateTime archivedAt;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */

    @TableLogic

    @TableField("deleted")

    private Integer deleted;

    /**
     * 创建时间
     */

    @TableField(fill = FieldFill.INSERT, value = "create_time")

    private LocalDateTime createTime;

    /**
     * 更新时间
     */

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_time")

    private LocalDateTime updateTime;

}
