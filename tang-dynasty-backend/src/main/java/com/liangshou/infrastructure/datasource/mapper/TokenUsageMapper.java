package com.liangshou.infrastructure.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liangshou.infrastructure.datasource.po.TokenUsagePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsagePO> {
}
