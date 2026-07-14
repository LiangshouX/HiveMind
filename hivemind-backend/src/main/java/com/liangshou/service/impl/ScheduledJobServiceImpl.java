package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.common.HmeBackendErrorCode;
import com.liangshou.common.utils.PageResult;
import com.liangshou.infrastructure.datasource.po.ScheduledJobPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobSupport;
import com.liangshou.service.IScheduledJobService;
import com.liangshou.service.dto.ScheduledJobDTO;
import com.liangshou.service.vo.ScheduledJobVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledJobServiceImpl implements IScheduledJobService {

    private final IScheduledJobSupport support;

    @Override
    public ScheduledJobVO getById(String userId, Long id) {
        ScheduledJobPO po = support.lambdaQuery()
                .eq(ScheduledJobPO::getId, id)
                .eq(ScheduledJobPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        ScheduledJobVO vo = new ScheduledJobVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<ScheduledJobVO> page(String userId, int current, int size) {
        Page<ScheduledJobPO> page = support.lambdaQuery()
                .eq(ScheduledJobPO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
        return PageResult.of(
                page.getTotal(),
                page.getRecords().stream().map(po -> {
                    ScheduledJobVO vo = new ScheduledJobVO();
                    vo.setId(po.getId());
                    return vo;
                }).collect(Collectors.toList()),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String userId, ScheduledJobDTO dto) {
        ScheduledJobPO po = new ScheduledJobPO();
        po.setId(dto.getId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, ScheduledJobDTO dto) {
        ScheduledJobPO po = support.lambdaQuery()
                .eq(ScheduledJobPO::getId, dto.getId())
                .eq(ScheduledJobPO::getUserId, Long.valueOf(userId))
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
        ScheduledJobPO po = support.lambdaQuery()
                .eq(ScheduledJobPO::getId, id)
                .eq(ScheduledJobPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new BizException(HmeBackendErrorCode.TASK_RECORD_NOT_FOUND_DELETE);
        }
        return support.removeById(id);
    }
}
