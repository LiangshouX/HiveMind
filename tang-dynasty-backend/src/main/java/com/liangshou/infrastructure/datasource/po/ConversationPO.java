package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_conversation")
public class ConversationPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String title;
    private String type;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
