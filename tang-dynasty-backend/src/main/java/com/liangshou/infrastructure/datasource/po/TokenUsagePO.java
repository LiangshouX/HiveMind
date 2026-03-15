package com.liangshou.infrastructure.datasource.po;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_token_usage")
public class TokenUsagePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate date;
    private String model;
    private Long tokens;
    private LocalDateTime createTime;
}
