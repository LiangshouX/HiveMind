package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.common.HmeBackendErrorCode;
import com.liangshou.common.utils.PageResult;
import com.liangshou.infrastructure.datasource.po.AgentTaskPO;
import com.liangshou.infrastructure.datasource.support.IAgentTaskSupport;
import com.liangshou.service.IAgentTaskService;
import com.liangshou.service.dto.AgentTaskDTO;
import com.liangshou.service.vo.AgentTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentTaskServiceImpl implements IAgentTaskService {

    private final IAgentTaskSupport support;

    @Override
    public AgentTaskVO getById(String userId, String taskId) {
        AgentTaskPO po = support.lambdaQuery()
                .eq(AgentTaskPO::getTaskId, taskId)
                .eq(AgentTaskPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        AgentTaskVO vo = new AgentTaskVO();
        vo.setTaskId(po.getTaskId());
        return vo;
    }

    @Override
    public PageResult<AgentTaskVO> page(String userId, int current, int size) {
        Page<AgentTaskPO> page = support.lambdaQuery()
                .eq(AgentTaskPO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
        return PageResult.of(
                page.getTotal(),
                page.getRecords().stream().map(po -> {
                    AgentTaskVO vo = new AgentTaskVO();
                    vo.setTaskId(po.getTaskId());
                    return vo;
                }).collect(Collectors.toList()),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String userId, AgentTaskDTO dto) {
        AgentTaskPO po = new AgentTaskPO();
        po.setTaskId(dto.getTaskId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, AgentTaskDTO dto) {
        AgentTaskPO po = support.lambdaQuery()
                .eq(AgentTaskPO::getTaskId, dto.getTaskId())
                .eq(AgentTaskPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new BizException(HmeBackendErrorCode.TASK_RECORD_NOT_FOUND_UPDATE);
        }
        po.setTaskId(dto.getTaskId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String userId, String taskId) {
        AgentTaskPO po = support.lambdaQuery()
                .eq(AgentTaskPO::getTaskId, taskId)
                .eq(AgentTaskPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new BizException(HmeBackendErrorCode.TASK_RECORD_NOT_FOUND_DELETE);
        }
        return support.removeById(taskId);
    }
}
