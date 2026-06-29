package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.TaskReportMapper;
import com.liangshou.infrastructure.datasource.po.TaskReportPO;
import com.liangshou.infrastructure.datasource.support.ITaskReportSupport;
import org.springframework.stereotype.Service;

@Service
public class TaskReportSupportImpl extends ServiceImpl<TaskReportMapper, TaskReportPO> implements ITaskReportSupport {
}
