package com.liangshou.service;

import com.liangshou.service.dto.AgentTaskDTO;
import com.liangshou.service.vo.AgentTaskVO;
import com.liangshou.common.utils.PageResult;

public interface IAgentTaskService {
    AgentTaskVO getById(String userId, String taskId);
    PageResult<AgentTaskVO> page(String userId, int current, int size);
    boolean save(String userId, AgentTaskDTO dto);
    boolean update(String userId, AgentTaskDTO dto);
    boolean delete(String userId, String taskId);
}
