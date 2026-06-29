package com.liangshou.service;

import com.liangshou.service.dto.TaskReportDTO;
import com.liangshou.service.vo.TaskReportVO;
import com.liangshou.common.utils.PageResult;

public interface ITaskReportService {
    TaskReportVO getById(String userId, Long id);
    PageResult<TaskReportVO> page(String userId, int current, int size);
    boolean save(String userId, TaskReportDTO dto);
    boolean update(String userId, TaskReportDTO dto);
    boolean delete(String userId, Long id);
}
