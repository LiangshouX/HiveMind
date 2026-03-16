package com.liangshou.infrastructure.datasource.support;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计实体基类
 * 
 * 所有需要审计字段（创建时间、更新时间）的实体必须继承此类
 */
@Data
public abstract class AuditableEntity {
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 预处理更新
     */
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
