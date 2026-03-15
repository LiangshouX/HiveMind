package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.TaskLogMapper;
import com.liangshou.infrastructure.datasource.po.TaskLogPO;
import com.liangshou.service.TaskLogService;
import org.springframework.stereotype.Service;

@Service
public class TaskLogServiceImpl extends ServiceImpl<TaskLogMapper, TaskLogPO> implements TaskLogService {
}
