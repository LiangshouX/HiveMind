package com.liangshou.service;

import com.liangshou.service.dto.SysChannelsDTO;
import com.liangshou.service.vo.SysChannelsVO;
import com.liangshou.common.utils.PageResult;

public interface ISysChannelsService {
    SysChannelsVO getById(String userId, Long id);
    PageResult<SysChannelsVO> page(String userId, int current, int size);
    boolean save(String userId, SysChannelsDTO dto);
    boolean update(String userId, SysChannelsDTO dto);
    boolean delete(String userId, Long id);
}
