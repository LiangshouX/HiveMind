package com.liangshou.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.OfficialMapper;
import com.liangshou.infrastructure.datasource.po.OfficialPO;
import com.liangshou.service.OfficialService;
import org.springframework.stereotype.Service;
@Service
public class OfficialServiceImpl extends ServiceImpl<OfficialMapper, OfficialPO> implements OfficialService {}
