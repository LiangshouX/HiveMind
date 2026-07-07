package com.liangshou.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器。
 * <p>
 * 为标注了 {@code FieldFill.INSERT} / {@code FieldFill.INSERT_UPDATE} 的字段
 * 自动注入创建时间和更新时间，无需在业务代码中手动赋值。
 */
@Slf4j
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        // 兼容 agent-engine 模块中使用 createdAt / created_at 命名的 PO
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
        // 兼容 record_time / delivery_time 等插入时填充的时间字段
        this.strictInsertFill(metaObject, "recordTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "deliveryTime", LocalDateTime::now, LocalDateTime.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        // 兼容 agent-engine 模块中使用 updatedAt 命名的 PO
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
    }
}
