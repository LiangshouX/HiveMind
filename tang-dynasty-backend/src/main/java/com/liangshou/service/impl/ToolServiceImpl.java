package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.ToolMapper;
import com.liangshou.infrastructure.datasource.po.ToolPO;
import com.liangshou.service.ToolService;
import org.springframework.stereotype.Service;

@Service
public class ToolServiceImpl extends ServiceImpl<ToolMapper, ToolPO> implements ToolService {
}
