package com.liangshou.service;

import com.liangshou.service.dto.EdictTasksDTO;
import com.liangshou.service.vo.EdictTasksVO;
import com.liangshou.common.utils.PageResult;

public interface IEdictTasksService {
    EdictTasksVO getById(String userId, String taskId);
    PageResult<EdictTasksVO> page(String userId, int current, int size);
    boolean save(String userId, EdictTasksDTO dto);
    boolean update(String userId, EdictTasksDTO dto);
    boolean delete(String userId, String taskId);
}
