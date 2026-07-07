package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务执行记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("scheduled_job_run_record")
public class ScheduledJobRunRecordPO implements Serializable {
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

         * 任务ID

         */

        @TableField("job_id")

        private String jobId;

    /**

     * 任务开始执行时间

     */

    @TableField("job_start_time")

    private LocalDateTime jobStartTime;

    /**

     * 任务执行结束时间

     */

    @TableField("job_finish_time")

    private LocalDateTime jobFinishTime;

    /**

     * 执行耗时(毫秒)

     */

    @TableField("duration_ms")

    private Long durationMs;

}
