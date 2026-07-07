package com.liangshou.service;

import com.liangshou.service.dto.SysTokenUsageDTO;
import com.liangshou.service.vo.SysTokenUsageVO;
import com.liangshou.common.utils.PageResult;

public interface ISysTokenUsageService {
    SysTokenUsageVO getById(String userId, Long id);
    PageResult<SysTokenUsageVO> page(String userId, int current, int size);
    boolean save(String userId, SysTokenUsageDTO dto);
    boolean update(String userId, SysTokenUsageDTO dto);
    boolean delete(String userId, Long id);
}
