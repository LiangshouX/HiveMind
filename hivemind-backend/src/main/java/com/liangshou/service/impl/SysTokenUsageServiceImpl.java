package com.liangshou.service.impl;

import com.liangshou.service.ISysTokenUsageService;
import com.liangshou.service.dto.SysTokenUsageDTO;
import com.liangshou.service.vo.SysTokenUsageVO;
import com.liangshou.infrastructure.datasource.po.SysTokenUsagePO;
import com.liangshou.infrastructure.datasource.support.ISysTokenUsageSupport;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
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
    public SysTokenUsageVO getById(String userId, Long id) {
        SysTokenUsagePO po = support.lambdaQuery()
                .eq(SysTokenUsagePO::getId, id)
                .eq(SysTokenUsagePO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        SysTokenUsageVO vo = new SysTokenUsageVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<SysTokenUsageVO> page(String userId, int current, int size) {
        Page<SysTokenUsagePO> page = support.lambdaQuery()
                .eq(SysTokenUsagePO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
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
    public boolean save(String userId, SysTokenUsageDTO dto) {
        SysTokenUsagePO po = new SysTokenUsagePO();
        po.setId(dto.getId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, SysTokenUsageDTO dto) {
        SysTokenUsagePO po = support.lambdaQuery()
                .eq(SysTokenUsagePO::getId, dto.getId())
                .eq(SysTokenUsagePO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限修改");
        }
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String userId, Long id) {
        SysTokenUsagePO po = support.lambdaQuery()
                .eq(SysTokenUsagePO::getId, id)
                .eq(SysTokenUsagePO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        return support.removeById(id);
    }
}
