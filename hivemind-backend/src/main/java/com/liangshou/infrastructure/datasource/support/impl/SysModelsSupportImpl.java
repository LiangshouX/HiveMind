package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.SysModelsMapper;
import com.liangshou.infrastructure.datasource.po.SysModelsPO;
import com.liangshou.infrastructure.datasource.support.ISysModelsSupport;
import org.springframework.stereotype.Service;

@Service
public class SysModelsSupportImpl extends ServiceImpl<SysModelsMapper, SysModelsPO> implements ISysModelsSupport {
}
