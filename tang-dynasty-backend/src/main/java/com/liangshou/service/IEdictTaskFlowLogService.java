package com.liangshou.service;

import com.liangshou.service.dto.EdictTaskFlowLogDTO;
import com.liangshou.service.vo.EdictTaskFlowLogVO;
import com.liangshou.common.utils.PageResult;

public interface IEdictTaskFlowLogService {
    EdictTaskFlowLogVO getById(Long id);
    PageResult<EdictTaskFlowLogVO> page(int current, int size);
    boolean save(EdictTaskFlowLogDTO dto);
    boolean update(EdictTaskFlowLogDTO dto);
    boolean delete(Long id);
}
