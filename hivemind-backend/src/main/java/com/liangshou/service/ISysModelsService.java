package com.liangshou.service;

import com.liangshou.service.dto.SysModelsDTO;
import com.liangshou.service.vo.SysModelsVO;
import com.liangshou.common.utils.PageResult;

import java.util.List;

public interface ISysModelsService {
    SysModelsVO getById(String userId, Long id);
    PageResult<SysModelsVO> page(String userId, int current, int size);
    List<SysModelsVO> listAll(String userId);
    SysModelsVO save(String userId, SysModelsDTO dto);
    boolean update(String userId, SysModelsDTO dto);
    boolean activate(String userId, Long id);
    boolean deactivate(String userId, Long id);
    boolean selectModel(String userId, Long id, String modelId, String modelName);
    boolean delete(String userId, Long id);
}
