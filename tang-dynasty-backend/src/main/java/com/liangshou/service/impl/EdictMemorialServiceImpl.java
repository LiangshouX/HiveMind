package com.liangshou.service.impl;

import com.liangshou.service.IEdictMemorialService;
import com.liangshou.service.dto.EdictMemorialDTO;
import com.liangshou.service.vo.EdictMemorialVO;
import com.liangshou.infrastructure.datasource.po.EdictMemorialPO;
import com.liangshou.infrastructure.datasource.support.IEdictMemorialSupport;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EdictMemorialServiceImpl implements IEdictMemorialService {

    private final IEdictMemorialSupport support;

    @Override
    public EdictMemorialVO getById(String userId, Long id) {
        EdictMemorialPO po = support.lambdaQuery()
                .eq(EdictMemorialPO::getId, id)
                .eq(EdictMemorialPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        EdictMemorialVO vo = new EdictMemorialVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<EdictMemorialVO> page(String userId, int current, int size) {
        Page<EdictMemorialPO> page = support.lambdaQuery()
                .eq(EdictMemorialPO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                EdictMemorialVO vo = new EdictMemorialVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(String userId, EdictMemorialDTO dto) {
        EdictMemorialPO po = new EdictMemorialPO();
        po.setId(dto.getId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, EdictMemorialDTO dto) {
        EdictMemorialPO po = support.lambdaQuery()
                .eq(EdictMemorialPO::getId, dto.getId())
                .eq(EdictMemorialPO::getUserId, Long.valueOf(userId))
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
        EdictMemorialPO po = support.lambdaQuery()
                .eq(EdictMemorialPO::getId, id)
                .eq(EdictMemorialPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        return support.removeById(id);
    }
}
