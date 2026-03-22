package com.liangshou.service.impl;

import com.liangshou.service.IScheduledJobRunRecordService;
import com.liangshou.service.dto.ScheduledJobRunRecordDTO;
import com.liangshou.service.vo.ScheduledJobRunRecordVO;
import com.liangshou.infrastructure.datasource.po.ScheduledJobRunRecordPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobRunRecordSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledJobRunRecordServiceImpl implements IScheduledJobRunRecordService {

    private final IScheduledJobRunRecordSupport support;

    @Override
    public ScheduledJobRunRecordVO getById(Long id) {
        ScheduledJobRunRecordPO po = support.getById(id);
        if (po == null) return null;
        ScheduledJobRunRecordVO vo = new ScheduledJobRunRecordVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<ScheduledJobRunRecordVO> page(int current, int size) {
        Page<ScheduledJobRunRecordPO> page = support.page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                ScheduledJobRunRecordVO vo = new ScheduledJobRunRecordVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ScheduledJobRunRecordDTO dto) {
        ScheduledJobRunRecordPO po = new ScheduledJobRunRecordPO();
        po.setId(dto.getId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ScheduledJobRunRecordDTO dto) {
        ScheduledJobRunRecordPO po = new ScheduledJobRunRecordPO();
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }
}
