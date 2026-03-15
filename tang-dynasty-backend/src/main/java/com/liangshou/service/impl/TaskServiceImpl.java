package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.TaskMapper;
import com.liangshou.infrastructure.datasource.po.TaskPO;
import com.liangshou.service.TaskService;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, TaskPO> implements TaskService {
}
