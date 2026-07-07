package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.AgentTaskMapper;
import com.liangshou.infrastructure.datasource.po.AgentTaskPO;
import com.liangshou.infrastructure.datasource.support.IAgentTaskSupport;
import org.springframework.stereotype.Service;

@Service
public class AgentTaskSupportImpl extends ServiceImpl<AgentTaskMapper, AgentTaskPO> implements IAgentTaskSupport {
}
