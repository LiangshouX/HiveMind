package com.liangshou.tangdynasty.agentic.infrastructure.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.po.TokenUsagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Token 使用量 Mapper
 */
@Mapper
public interface TokenUsageMapper extends BaseMapper<TokenUsagePO> {
}
