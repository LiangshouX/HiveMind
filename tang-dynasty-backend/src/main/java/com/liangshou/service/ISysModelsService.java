package com.liangshou.service;

import com.liangshou.service.dto.SysModelsDTO;
import com.liangshou.service.vo.SysModelsVO;
import com.liangshou.common.utils.PageResult;

public interface ISysModelsService {
    SysModelsVO getById(Long id);
    PageResult<SysModelsVO> page(int current, int size);
    boolean save(SysModelsDTO dto);
    boolean update(SysModelsDTO dto);
    boolean delete(Long id);
}
