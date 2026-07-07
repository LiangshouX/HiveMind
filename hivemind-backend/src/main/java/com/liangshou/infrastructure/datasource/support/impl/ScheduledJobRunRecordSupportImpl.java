package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.ScheduledJobRunRecordMapper;
import com.liangshou.infrastructure.datasource.po.ScheduledJobRunRecordPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobRunRecordSupport;
import org.springframework.stereotype.Service;

@Service
public class ScheduledJobRunRecordSupportImpl extends ServiceImpl<ScheduledJobRunRecordMapper, ScheduledJobRunRecordPO> implements IScheduledJobRunRecordSupport {
}
