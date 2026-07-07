package com.liangshou.service.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SysUserVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userId;
    private String nickname;
    private String role;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
