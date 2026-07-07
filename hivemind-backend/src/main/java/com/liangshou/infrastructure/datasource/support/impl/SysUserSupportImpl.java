package com.liangshou.infrastructure.datasource.support.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.SysUserMapper;
import com.liangshou.infrastructure.datasource.po.SysUserPO;
import com.liangshou.infrastructure.datasource.support.ISysUserSupport;
import org.springframework.stereotype.Service;

@Service
public class SysUserSupportImpl extends ServiceImpl<SysUserMapper, SysUserPO> implements ISysUserSupport {
}
