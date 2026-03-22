package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 奏折/任务审批表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("edict_memorial")
public class EdictMemorialPO implements Serializable {
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

     * 任务ID, 对应 edict_tasks 的任务id

     */

    @TableField("task_id")

    private String taskId;

    /**

     * 任务标题

     */

    @TableField("task_title")

    private String taskTitle;

    /**

     * 任务指令内容

     */

    @TableField("task_content")

    private String taskContent;

    /**

     * 任务输出结果

     */

    @TableField("task_result")

    private String taskResult;

    /**

     * 批阅状态: WAITING/APPROVAL/REFUSAL/REDO

     */

    @TableField("approval_state")

    private String approvalState;

    /**

     * 奏折递交时间/创建时间

     */

    @TableField(fill = FieldFill.INSERT, value = "delivery_time")

    private LocalDateTime deliveryTime;

}
