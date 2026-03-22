package com.liangshou.service.impl;

import com.liangshou.service.ISysTokenUsageService;
import com.liangshou.service.dto.SysTokenUsageDTO;
import com.liangshou.service.vo.SysTokenUsageVO;
import com.liangshou.infrastructure.datasource.po.SysTokenUsagePO;
import com.liangshou.infrastructure.datasource.support.ISysTokenUsageSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysTokenUsageServiceImpl implements ISysTokenUsageService {

    private final ISysTokenUsageSupport support;

    @Override
    public SysTokenUsageVO getById(Long id) {
        SysTokenUsagePO po = support.getById(id);
        if (po == null) return null;
        SysTokenUsageVO vo = new SysTokenUsageVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<SysTokenUsageVO> page(int current, int size) {
        Page<SysTokenUsagePO> page = support.page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                SysTokenUsageVO vo = new SysTokenUsageVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysTokenUsageDTO dto) {
        SysTokenUsagePO po = new SysTokenUsagePO();
        po.setId(dto.getId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SysTokenUsageDTO dto) {
        SysTokenUsagePO po = new SysTokenUsagePO();
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }
}
