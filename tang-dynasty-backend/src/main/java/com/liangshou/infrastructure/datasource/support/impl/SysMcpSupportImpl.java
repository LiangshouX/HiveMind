package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.SysMcpMapper;
import com.liangshou.infrastructure.datasource.po.SysMcpPO;
import com.liangshou.infrastructure.datasource.support.ISysMcpSupport;
import org.springframework.stereotype.Service;

@Service
public class SysMcpSupportImpl extends ServiceImpl<SysMcpMapper, SysMcpPO> implements ISysMcpSupport {
}
