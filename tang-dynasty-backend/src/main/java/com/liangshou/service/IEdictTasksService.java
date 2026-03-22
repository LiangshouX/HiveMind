package com.liangshou.service;

import com.liangshou.service.dto.EdictTasksDTO;
import com.liangshou.service.vo.EdictTasksVO;
import com.liangshou.common.utils.PageResult;

public interface IEdictTasksService {
    EdictTasksVO getById(String taskId);
    PageResult<EdictTasksVO> page(int current, int size);
    boolean save(EdictTasksDTO dto);
    boolean update(EdictTasksDTO dto);
    boolean delete(String taskId);
}
