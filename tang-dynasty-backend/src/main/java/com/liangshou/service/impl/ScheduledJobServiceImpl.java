package com.liangshou.service.impl;

import com.liangshou.service.IScheduledJobService;
import com.liangshou.service.dto.ScheduledJobDTO;
import com.liangshou.service.vo.ScheduledJobVO;
import com.liangshou.infrastructure.datasource.po.ScheduledJobPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledJobServiceImpl implements IScheduledJobService {

    private final IScheduledJobSupport support;

    @Override
    public ScheduledJobVO getById(Long id) {
        ScheduledJobPO po = support.getById(id);
        if (po == null) return null;
        ScheduledJobVO vo = new ScheduledJobVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<ScheduledJobVO> page(int current, int size) {
        Page<ScheduledJobPO> page = support.page(new Page<>(current, size));
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
    public boolean save(ScheduledJobDTO dto) {
        ScheduledJobPO po = new ScheduledJobPO();
        po.setId(dto.getId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ScheduledJobDTO dto) {
        ScheduledJobPO po = new ScheduledJobPO();
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }
}
