package com.liangshou.service;

import com.liangshou.service.dto.SysUserDTO;
import com.liangshou.service.vo.SysUserVO;
import com.liangshou.common.utils.PageResult;

public interface ISysUserService {
    SysUserVO getById(Long id);
    SysUserVO getByUserId(String userId);
    PageResult<SysUserVO> page(int current, int size);
    SysUserVO register(SysUserDTO dto);
    SysUserVO updateProfile(String currentUserId, SysUserDTO dto);
    boolean save(SysUserDTO dto);
    boolean update(SysUserDTO dto);
    boolean delete(Long id);
}
