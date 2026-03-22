package com.liangshou.service.impl;

import com.liangshou.service.IEdictTaskFlowLogService;
import com.liangshou.service.dto.EdictTaskFlowLogDTO;
import com.liangshou.service.vo.EdictTaskFlowLogVO;
import com.liangshou.infrastructure.datasource.po.EdictTaskFlowLogPO;
import com.liangshou.infrastructure.datasource.support.IEdictTaskFlowLogSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EdictTaskFlowLogServiceImpl implements IEdictTaskFlowLogService {

    private final IEdictTaskFlowLogSupport support;

    @Override
    public EdictTaskFlowLogVO getById(Long id) {
        EdictTaskFlowLogPO po = support.getById(id);
        if (po == null) return null;
        EdictTaskFlowLogVO vo = new EdictTaskFlowLogVO();
        vo.setId(po.getId());
        return vo;
    }

    @Override
    public PageResult<EdictTaskFlowLogVO> page(int current, int size) {
        Page<EdictTaskFlowLogPO> page = support.page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                EdictTaskFlowLogVO vo = new EdictTaskFlowLogVO();
                vo.setId(po.getId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EdictTaskFlowLogDTO dto) {
        EdictTaskFlowLogPO po = new EdictTaskFlowLogPO();
        po.setId(dto.getId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(EdictTaskFlowLogDTO dto) {
        EdictTaskFlowLogPO po = new EdictTaskFlowLogPO();
        po.setId(dto.getId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        return support.removeById(id);
    }
}
