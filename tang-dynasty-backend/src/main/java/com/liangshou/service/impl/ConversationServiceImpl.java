package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liangshou.infrastructure.datasource.mapper.ConversationMapper;
import com.liangshou.infrastructure.datasource.po.ConversationPO;
import com.liangshou.service.ConversationService;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, ConversationPO> implements ConversationService {
}
