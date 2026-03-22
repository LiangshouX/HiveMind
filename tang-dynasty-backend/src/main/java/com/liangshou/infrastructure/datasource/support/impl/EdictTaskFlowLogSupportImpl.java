package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.EdictTaskFlowLogMapper;
import com.liangshou.infrastructure.datasource.po.EdictTaskFlowLogPO;
import com.liangshou.infrastructure.datasource.support.IEdictTaskFlowLogSupport;
import org.springframework.stereotype.Service;

@Service
public class EdictTaskFlowLogSupportImpl extends ServiceImpl<EdictTaskFlowLogMapper, EdictTaskFlowLogPO> implements IEdictTaskFlowLogSupport {
}
