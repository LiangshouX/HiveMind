package com.liangshou.service.impl;

import com.liangshou.service.ISysModelsService;
import com.liangshou.service.dto.SysModelsDTO;
import com.liangshou.service.vo.SysModelsVO;
import com.liangshou.infrastructure.datasource.po.SysModelsPO;
import com.liangshou.infrastructure.datasource.support.ISysModelsSupport;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysModelsServiceImpl implements ISysModelsService {

    private final ISysModelsSupport support;

    @Override
    public SysModelsVO getById(String userId, Long id) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, id)
                .eq(SysModelsPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        SysModelsVO vo = new SysModelsVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<SysModelsVO> page(String userId, int current, int size) {
        Page<SysModelsPO> page = support.lambdaQuery()
                .eq(SysModelsPO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                SysModelsVO vo = new SysModelsVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String userId, SysModelsDTO dto) {
        SysModelsPO po = new SysModelsPO();
        po.setId(dto.getId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, SysModelsDTO dto) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, dto.getId())
                .eq(SysModelsPO::getUserId, Long.valueOf(userId))
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
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, id)
                .eq(SysModelsPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        return support.removeById(id);
    }
}
