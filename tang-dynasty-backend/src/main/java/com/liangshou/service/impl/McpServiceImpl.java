package com.liangshou.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.McpMapper;
import com.liangshou.infrastructure.datasource.po.McpPO;
import com.liangshou.service.McpService;
import org.springframework.stereotype.Service;
@Service
public class McpServiceImpl extends ServiceImpl<McpMapper, McpPO> implements McpService {}
