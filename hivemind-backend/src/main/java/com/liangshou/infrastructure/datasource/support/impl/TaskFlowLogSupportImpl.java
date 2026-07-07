package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.TaskFlowLogMapper;
import com.liangshou.infrastructure.datasource.po.TaskFlowLogPO;
import com.liangshou.infrastructure.datasource.support.ITaskFlowLogSupport;
import org.springframework.stereotype.Service;

@Service
public class TaskFlowLogSupportImpl extends ServiceImpl<TaskFlowLogMapper, TaskFlowLogPO> implements ITaskFlowLogSupport {
}
