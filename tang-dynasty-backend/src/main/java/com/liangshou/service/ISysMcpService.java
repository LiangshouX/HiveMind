package com.liangshou.service;

import com.liangshou.service.dto.SysMcpDTO;
import com.liangshou.service.vo.SysMcpVO;
import com.liangshou.common.utils.PageResult;

public interface ISysMcpService {
    SysMcpVO getById(String userId, Long id);
    PageResult<SysMcpVO> page(String userId, int current, int size);
    boolean save(String userId, SysMcpDTO dto);
    boolean update(String userId, SysMcpDTO dto);
    boolean delete(String userId, Long id);
}
