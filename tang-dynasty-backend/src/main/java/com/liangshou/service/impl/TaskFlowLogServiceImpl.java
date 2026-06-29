package com.liangshou.service.impl;

import com.liangshou.service.ITaskFlowLogService;
import com.liangshou.service.dto.TaskFlowLogDTO;
import com.liangshou.service.vo.TaskFlowLogVO;
import com.liangshou.infrastructure.datasource.po.TaskFlowLogPO;
import com.liangshou.infrastructure.datasource.support.ITaskFlowLogSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskFlowLogServiceImpl implements ITaskFlowLogService {

    private final ITaskFlowLogSupport support;

    @Override
    public TaskFlowLogVO getById(Long id) {
        TaskFlowLogPO po = support.getById(id);
        if (po == null) return null;
        TaskFlowLogVO vo = new TaskFlowLogVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<TaskFlowLogVO> page(int current, int size) {
        Page<TaskFlowLogPO> page = support.page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                TaskFlowLogVO vo = new TaskFlowLogVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TaskFlowLogDTO dto) {
        TaskFlowLogPO po = new TaskFlowLogPO();
        po.setId(dto.getId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(TaskFlowLogDTO dto) {
        TaskFlowLogPO po = new TaskFlowLogPO();
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }
}
