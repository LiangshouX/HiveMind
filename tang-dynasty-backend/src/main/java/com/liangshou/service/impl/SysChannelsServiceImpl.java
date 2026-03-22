package com.liangshou.service.impl;

import com.liangshou.service.ISysChannelsService;
import com.liangshou.service.dto.SysChannelsDTO;
import com.liangshou.service.vo.SysChannelsVO;
import com.liangshou.infrastructure.datasource.po.SysChannelsPO;
import com.liangshou.infrastructure.datasource.support.ISysChannelsSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysChannelsServiceImpl implements ISysChannelsService {

    private final ISysChannelsSupport support;

    @Override
    public SysChannelsVO getById(Long id) {
        SysChannelsPO po = support.getById(id);
        if (po == null) return null;
        SysChannelsVO vo = new SysChannelsVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<SysChannelsVO> page(int current, int size) {
        Page<SysChannelsPO> page = support.page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                SysChannelsVO vo = new SysChannelsVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysChannelsDTO dto) {
        SysChannelsPO po = new SysChannelsPO();
        po.setId(dto.getId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SysChannelsDTO dto) {
        SysChannelsPO po = new SysChannelsPO();
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }
}
