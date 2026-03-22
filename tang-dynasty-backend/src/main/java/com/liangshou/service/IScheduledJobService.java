package com.liangshou.service;

import com.liangshou.service.dto.ScheduledJobDTO;
import com.liangshou.service.vo.ScheduledJobVO;
import com.liangshou.common.utils.PageResult;

public interface IScheduledJobService {
    ScheduledJobVO getById(Long id);
    PageResult<ScheduledJobVO> page(int current, int size);
    boolean save(ScheduledJobDTO dto);
    boolean update(ScheduledJobDTO dto);
    boolean delete(Long id);
}
