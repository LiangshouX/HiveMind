package com.liangshou.service;

import com.liangshou.service.dto.TaskFlowLogDTO;
import com.liangshou.service.vo.TaskFlowLogVO;
import com.liangshou.common.utils.PageResult;

public interface ITaskFlowLogService {
    TaskFlowLogVO getById(Long id);
    PageResult<TaskFlowLogVO> page(int current, int size);
    boolean save(TaskFlowLogDTO dto);
    boolean update(TaskFlowLogDTO dto);
    boolean delete(Long id);
}
