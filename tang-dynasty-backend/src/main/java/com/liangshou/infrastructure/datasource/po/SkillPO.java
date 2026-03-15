package com.liangshou.infrastructure.datasource.po;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "sys_skill")
public class SkillPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String script;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
