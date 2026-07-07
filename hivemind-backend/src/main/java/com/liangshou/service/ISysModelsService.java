package com.liangshou.service;

import com.liangshou.service.dto.SysModelsDTO;
import com.liangshou.service.vo.SysModelsVO;
import com.liangshou.common.utils.PageResult;

public interface ISysModelsService {
    SysModelsVO getById(String userId, Long id);
    PageResult<SysModelsVO> page(String userId, int current, int size);
    boolean save(String userId, SysModelsDTO dto);
    boolean update(String userId, SysModelsDTO dto);
    boolean delete(String userId, Long id);
}
