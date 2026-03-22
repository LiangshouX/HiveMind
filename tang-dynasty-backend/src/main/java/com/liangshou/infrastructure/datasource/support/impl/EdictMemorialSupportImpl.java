package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.EdictMemorialMapper;
import com.liangshou.infrastructure.datasource.po.EdictMemorialPO;
import com.liangshou.infrastructure.datasource.support.IEdictMemorialSupport;
import org.springframework.stereotype.Service;

@Service
public class EdictMemorialSupportImpl extends ServiceImpl<EdictMemorialMapper, EdictMemorialPO> implements IEdictMemorialSupport {
}
