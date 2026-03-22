package com.liangshou.service;

import com.liangshou.service.dto.ScheduledJobRunRecordDTO;
import com.liangshou.service.vo.ScheduledJobRunRecordVO;
import com.liangshou.common.utils.PageResult;

public interface IScheduledJobRunRecordService {
    ScheduledJobRunRecordVO getById(Long id);
    PageResult<ScheduledJobRunRecordVO> page(int current, int size);
    boolean save(ScheduledJobRunRecordDTO dto);
    boolean update(ScheduledJobRunRecordDTO dto);
    boolean delete(Long id);
}
