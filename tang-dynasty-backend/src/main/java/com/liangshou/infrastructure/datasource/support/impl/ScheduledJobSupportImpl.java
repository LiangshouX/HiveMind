package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.ScheduledJobMapper;
import com.liangshou.infrastructure.datasource.po.ScheduledJobPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobSupport;
import org.springframework.stereotype.Service;

@Service
public class ScheduledJobSupportImpl extends ServiceImpl<ScheduledJobMapper, ScheduledJobPO> implements IScheduledJobSupport {
}
