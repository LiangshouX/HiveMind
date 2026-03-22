package com.liangshou.service.impl;

import com.liangshou.service.ISysUserService;
import com.liangshou.service.dto.SysUserDTO;
import com.liangshou.service.vo.SysUserVO;
import com.liangshou.infrastructure.datasource.po.SysUserPO;
import com.liangshou.infrastructure.datasource.support.ISysUserSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private final ISysUserSupport support;

    @Override
    public SysUserVO getById(Long id) {
        SysUserPO po = support.getById(id);
        if (po == null) return null;
        SysUserVO vo = new SysUserVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<SysUserVO> page(int current, int size) {
        Page<SysUserPO> page = support.page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                SysUserVO vo = new SysUserVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysUserDTO dto) {
        SysUserPO po = new SysUserPO();
        po.setId(dto.getId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SysUserDTO dto) {
        SysUserPO po = new SysUserPO();
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }
}
