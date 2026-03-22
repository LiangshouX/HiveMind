package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.EdictTasksMapper;
import com.liangshou.infrastructure.datasource.po.EdictTasksPO;
import com.liangshou.infrastructure.datasource.support.IEdictTasksSupport;
import org.springframework.stereotype.Service;

@Service
public class EdictTasksSupportImpl extends ServiceImpl<EdictTasksMapper, EdictTasksPO> implements IEdictTasksSupport {
}
