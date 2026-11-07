package com.liangshou.service.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务视图对象。
 *
 * @author LiangshouX
 */
@Data
public class ScheduledJobVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 业务任务ID */
    private String jobId;

    /** 任务名称 */
    private String jobName;

    /** 是否启用：0-否 1-是 */
    private Integer isActivated;

    /** cron 表达式 */
    private String cronConfig;

    /** 时区 */
    private String timeZone;

    /** 任务描述 */
    private String jobDescription;

    /** 定时任务驱动的业务任务ID */
    private String jobDrivedTaskId;

    /** 创建时间 */
    private String createTime;

    /** 更新时间 */
    private String updateTime;
}
