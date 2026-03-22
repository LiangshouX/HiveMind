package com.liangshou.service;

import com.liangshou.service.dto.SysMcpDTO;
import com.liangshou.service.vo.SysMcpVO;
import com.liangshou.common.utils.PageResult;

public interface ISysMcpService {
    SysMcpVO getById(Long id);
    PageResult<SysMcpVO> page(int current, int size);
    boolean save(SysMcpDTO dto);
    boolean update(SysMcpDTO dto);
    boolean delete(Long id);
}
