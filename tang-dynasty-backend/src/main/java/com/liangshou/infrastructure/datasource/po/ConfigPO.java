package com.liangshou.infrastructure.datasource.po;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class ConfigPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String keyName;
    private String value;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
