package com.liangshou.service.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SysUserDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userId;
    private String password;
    private String nickname;
    private String role;
}
