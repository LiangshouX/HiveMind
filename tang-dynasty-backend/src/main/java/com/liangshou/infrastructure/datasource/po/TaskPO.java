package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "sys_task", autoResultMap = true)
public class TaskPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String official;
    private String department;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> payload;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> result;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
