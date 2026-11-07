package com.liangshou.service.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务数据传输对象。
 *
 * <p>用于新增/更新定时任务调度配置。</p>
 *
 * @author LiangshouX
 */
@Data
public class ScheduledJobDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（更新时必填） */
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

    /** 任务驱动内容（JSON 字符串） */
    private String jobRequireContent;

    /** 定时任务驱动的业务任务ID */
    private String jobDrivedTaskId;
}
