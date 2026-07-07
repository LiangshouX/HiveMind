package com.liangshou.service;

import com.liangshou.service.dto.ScheduledJobDTO;
import com.liangshou.service.vo.ScheduledJobVO;
import com.liangshou.common.utils.PageResult;

public interface IScheduledJobService {
    ScheduledJobVO getById(String userId, Long id);
    PageResult<ScheduledJobVO> page(String userId, int current, int size);
    boolean save(String userId, ScheduledJobDTO dto);
    boolean update(String userId, ScheduledJobDTO dto);
    boolean delete(String userId, Long id);
}
