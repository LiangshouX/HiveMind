package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.SysChannelsMapper;
import com.liangshou.infrastructure.datasource.po.SysChannelsPO;
import com.liangshou.infrastructure.datasource.support.ISysChannelsSupport;
import org.springframework.stereotype.Service;

@Service
public class SysChannelsSupportImpl extends ServiceImpl<SysChannelsMapper, SysChannelsPO> implements ISysChannelsSupport {
}
