package com.liangshou.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.ModelMapper;
import com.liangshou.infrastructure.datasource.po.ModelPO;
import com.liangshou.service.ModelService;
import org.springframework.stereotype.Service;
@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, ModelPO> implements ModelService {}
