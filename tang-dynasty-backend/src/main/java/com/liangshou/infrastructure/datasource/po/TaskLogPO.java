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
@TableName(value = "sys_task_log", autoResultMap = true)
public class TaskLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String type;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;
    private LocalDateTime createTime;
}
