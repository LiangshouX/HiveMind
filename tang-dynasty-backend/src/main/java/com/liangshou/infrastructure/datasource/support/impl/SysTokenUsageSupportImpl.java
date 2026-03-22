package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.SysTokenUsageMapper;
import com.liangshou.infrastructure.datasource.po.SysTokenUsagePO;
import com.liangshou.infrastructure.datasource.support.ISysTokenUsageSupport;
import org.springframework.stereotype.Service;

@Service
public class SysTokenUsageSupportImpl extends ServiceImpl<SysTokenUsageMapper, SysTokenUsagePO> implements ISysTokenUsageSupport {
}
