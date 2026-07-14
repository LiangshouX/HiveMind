package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.common.HmeBackendErrorCode;
import com.liangshou.common.utils.PageResult;
import com.liangshou.infrastructure.datasource.po.TaskReportPO;
import com.liangshou.infrastructure.datasource.support.ITaskReportSupport;
import com.liangshou.service.ITaskReportService;
import com.liangshou.service.dto.TaskReportDTO;
import com.liangshou.service.vo.TaskReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskReportServiceImpl implements ITaskReportService {

    private final ITaskReportSupport support;

    @Override
    public TaskReportVO getById(String userId, Long id) {
        TaskReportPO po = support.lambdaQuery()
                .eq(TaskReportPO::getId, id)
                .eq(TaskReportPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        TaskReportVO vo = new TaskReportVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<TaskReportVO> page(String userId, int current, int size) {
        Page<TaskReportPO> page = support.lambdaQuery()
                .eq(TaskReportPO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
        return PageResult.of(
                page.getTotal(),
                page.getRecords().stream().map(po -> {
                    TaskReportVO vo = new TaskReportVO();
                    vo.setId(po.getId());
                    return vo;
                }).collect(Collectors.toList()),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String userId, TaskReportDTO dto) {
        TaskReportPO po = new TaskReportPO();
        po.setId(dto.getId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, TaskReportDTO dto) {
        TaskReportPO po = support.lambdaQuery()
                .eq(TaskReportPO::getId, dto.getId())
                .eq(TaskReportPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new BizException(HmeBackendErrorCode.TASK_RECORD_NOT_FOUND_UPDATE);
        }
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String userId, Long id) {
        TaskReportPO po = support.lambdaQuery()
                .eq(TaskReportPO::getId, id)
                .eq(TaskReportPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new BizException(HmeBackendErrorCode.TASK_RECORD_NOT_FOUND_DELETE);
        }
        return support.removeById(id);
    }
}
