package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "td_task")
public class TaskPO {
    
    @TableId(type = IdType.INPUT)
    private String id;
    
    private String title;
    private Long officialId;
    private Long deptId;
    private String state;
    private String priority;
    private String blockReason;
    private Integer reviewRound;
    private String prevState;
    private String outputResult;
    private String acCriteria;
    private Boolean archived;
    private LocalDateTime archivedAt;
    
    @TableLogic
    private Integer deleted;
    
    @Version
    private Integer version;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
