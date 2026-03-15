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
@TableName(value = "sys_tool", autoResultMap = true)
public class ToolPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
