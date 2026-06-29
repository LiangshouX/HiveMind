package com.liangshou.infrastructure.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liangshou.infrastructure.datasource.po.TaskFlowLogPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskFlowLogMapper extends BaseMapper<TaskFlowLogPO> {
}
