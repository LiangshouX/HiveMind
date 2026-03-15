package com.liangshou.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.SkillMapper;
import com.liangshou.infrastructure.datasource.po.SkillPO;
import com.liangshou.service.SkillService;
import org.springframework.stereotype.Service;
@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, SkillPO> implements SkillService {}
