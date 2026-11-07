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

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledJobServiceImpl implements IScheduledJobService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IScheduledJobSupport support;

    @Override
    public ScheduledJobVO getById(String userId, Long id) {
        ScheduledJobPO po = support.lambdaQuery()
                .eq(ScheduledJobPO::getId, id)
                .eq(ScheduledJobPO::getUserId, userId)
                .one();
        if (po == null) return null;
        return toVO(po);
    }

    @Override
    public PageResult<ScheduledJobVO> page(String userId, int current, int size) {
        Page<ScheduledJobPO> page = support.lambdaQuery()
                .eq(ScheduledJobPO::getUserId, userId)
                .orderByDesc(ScheduledJobPO::getUpdateTime)
                .page(new Page<>(current, size));
        return PageResult.of(
                page.getTotal(),
                page.getRecords().stream().map(this::toVO).collect(Collectors.toList()),
                page.getCurrent(),
                page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String userId, ScheduledJobDTO dto) {
        ScheduledJobPO po = new ScheduledJobPO();
        po.setUserId(userId);
        applyDto(po, dto);
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, ScheduledJobDTO dto) {
        ScheduledJobPO po = support.lambdaQuery()
                .eq(ScheduledJobPO::getId, dto.getId())
                .eq(ScheduledJobPO::getUserId, userId)
                .one();
        if (po == null) {
            throw new BizException(HmeBackendErrorCode.TASK_RECORD_NOT_FOUND_UPDATE);
        }
        applyDto(po, dto);
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String userId, Long id) {
        ScheduledJobPO po = support.lambdaQuery()
                .eq(ScheduledJobPO::getId, id)
                .eq(ScheduledJobPO::getUserId, userId)
                .one();
        if (po == null) {
            throw new BizException(HmeBackendErrorCode.TASK_RECORD_NOT_FOUND_DELETE);
        }
        return support.removeById(id);
    }

    /**
     * 将 DTO 字段写入 PO（忽略 null 值，避免部分更新覆盖已有配置）。
     */
    private void applyDto(ScheduledJobPO po, ScheduledJobDTO dto) {
        if (dto.getJobId() != null) po.setJobId(dto.getJobId());
        if (dto.getJobName() != null) po.setJobName(dto.getJobName());
        if (dto.getIsActivated() != null) po.setIsActivated(dto.getIsActivated());
        if (dto.getCronConfig() != null) po.setCronConfig(dto.getCronConfig());
        if (dto.getTimeZone() != null) po.setTimeZone(dto.getTimeZone());
        if (dto.getJobDescription() != null) po.setJobDescription(dto.getJobDescription());
        if (dto.getJobRequireContent() != null) po.setJobRequireContent(dto.getJobRequireContent());
        if (dto.getJobDrivedTaskId() != null) po.setJobDrivedTaskId(dto.getJobDrivedTaskId());
    }

    /**
     * PO 转 VO。
     */
    private ScheduledJobVO toVO(ScheduledJobPO po) {
        ScheduledJobVO vo = new ScheduledJobVO();
        vo.setId(po.getId());
        vo.setJobId(po.getJobId());
        vo.setJobName(po.getJobName());
        vo.setIsActivated(po.getIsActivated());
        vo.setCronConfig(po.getCronConfig());
        vo.setTimeZone(po.getTimeZone());
        vo.setJobDescription(po.getJobDescription());
        vo.setJobDrivedTaskId(po.getJobDrivedTaskId());
        vo.setCreateTime(po.getCreateTime() == null ? null : po.getCreateTime().format(TIME_FORMATTER));
        vo.setUpdateTime(po.getUpdateTime() == null ? null : po.getUpdateTime().format(TIME_FORMATTER));
        return vo;
    }
}
