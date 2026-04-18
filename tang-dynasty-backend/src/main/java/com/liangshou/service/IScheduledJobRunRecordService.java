package com.liangshou.service;

import com.liangshou.service.dto.ScheduledJobRunRecordDTO;
import com.liangshou.service.vo.ScheduledJobRunRecordVO;
import com.liangshou.common.utils.PageResult;

public interface IScheduledJobRunRecordService {
    ScheduledJobRunRecordVO getById(String userId, Long id);
    PageResult<ScheduledJobRunRecordVO> page(String userId, int current, int size);
    boolean save(String userId, ScheduledJobRunRecordDTO dto);
    boolean update(String userId, ScheduledJobRunRecordDTO dto);
    boolean delete(String userId, Long id);
}
