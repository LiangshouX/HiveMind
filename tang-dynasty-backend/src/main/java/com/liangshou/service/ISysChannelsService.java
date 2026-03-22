package com.liangshou.service;

import com.liangshou.service.dto.SysChannelsDTO;
import com.liangshou.service.vo.SysChannelsVO;
import com.liangshou.common.utils.PageResult;

public interface ISysChannelsService {
    SysChannelsVO getById(Long id);
    PageResult<SysChannelsVO> page(int current, int size);
    boolean save(SysChannelsDTO dto);
    boolean update(SysChannelsDTO dto);
    boolean delete(Long id);
}
