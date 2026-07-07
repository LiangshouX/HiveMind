package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务管理表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("scheduled_job")
public class ScheduledJobPO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**

     * 主键ID

     */

    @TableId(type = IdType.AUTO)
    private Long id;

        /**

         * 任务ID

         */

        @TableField("job_id")

        private String jobId;

    /**

     * 任务名称

     */

    @TableField("job_name")

    private String jobName;

    /**

     * 任务所属的用户ID

     */

    @TableField("user_id")

    private Long userId;

    /**

     * 是否开启: 0-否, 1-是

     */

    @TableField("is_activated")

    private Integer isActivated;

    /**

     * 定时任务cron表达式

     */

    @TableField("cron_config")

    private String cronConfig;

    /**

     * 时区

     */

    @TableField("time_zone")

    private String timeZone;

    /**

     * 任务描述

     */

    @TableField("job_description")

    private String jobDescription;

    /**

     * 请求内容, Json串描述的message

     */

    @TableField("job_require_content")

    private String jobRequireContent;

    /**

     * 定时任务驱动的任务ID

     */

    @TableField("job_drived_task_id")

    private String jobDrivedTaskId;

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
