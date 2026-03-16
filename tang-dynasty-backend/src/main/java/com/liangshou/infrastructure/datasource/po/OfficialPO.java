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
@TableName(value = "sys_official", autoResultMap = true)
public class OfficialPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String title;
    private String department;
    private String bio;
    private String prompt;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> modelConfig;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private java.util.List<String> skills;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
