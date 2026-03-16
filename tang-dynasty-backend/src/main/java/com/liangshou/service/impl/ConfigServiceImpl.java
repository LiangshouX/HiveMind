package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.ConfigMapper;
import com.liangshou.infrastructure.datasource.po.ConfigPO;
import com.liangshou.service.ConfigService;
import org.springframework.stereotype.Service;

@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, ConfigPO> implements ConfigService {
}
