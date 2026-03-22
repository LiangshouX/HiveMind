package com.liangshou.service;

import com.liangshou.service.dto.SysTokenUsageDTO;
import com.liangshou.service.vo.SysTokenUsageVO;
import com.liangshou.common.utils.PageResult;

public interface ISysTokenUsageService {
    SysTokenUsageVO getById(Long id);
    PageResult<SysTokenUsageVO> page(int current, int size);
    boolean save(SysTokenUsageDTO dto);
    boolean update(SysTokenUsageDTO dto);
    boolean delete(Long id);
}
