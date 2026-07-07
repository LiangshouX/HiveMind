package com.liangshou.service.impl;

import com.liangshou.service.ISysMcpService;
import com.liangshou.service.dto.SysMcpDTO;
import com.liangshou.service.vo.SysMcpVO;
import com.liangshou.infrastructure.datasource.po.SysMcpPO;
import com.liangshou.infrastructure.datasource.support.ISysMcpSupport;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMcpServiceImpl implements ISysMcpService {

    private final ISysMcpSupport support;

    @Override
    public SysMcpVO getById(String userId, Long id) {
        SysMcpPO po = support.lambdaQuery()
                .eq(SysMcpPO::getId, id)
                .eq(SysMcpPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        SysMcpVO vo = new SysMcpVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<SysMcpVO> page(String userId, int current, int size) {
        Page<SysMcpPO> page = support.lambdaQuery()
                .eq(SysMcpPO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                SysMcpVO vo = new SysMcpVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String userId, SysMcpDTO dto) {
        SysMcpPO po = new SysMcpPO();
        po.setId(dto.getId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, SysMcpDTO dto) {
        SysMcpPO po = support.lambdaQuery()
                .eq(SysMcpPO::getId, dto.getId())
                .eq(SysMcpPO::getUserId, Long.valueOf(userId))
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
        SysMcpPO po = support.lambdaQuery()
                .eq(SysMcpPO::getId, id)
                .eq(SysMcpPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        return support.removeById(id);
    }
}
