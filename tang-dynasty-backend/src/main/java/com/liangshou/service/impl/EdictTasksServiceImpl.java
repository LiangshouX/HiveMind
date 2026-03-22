package com.liangshou.service.impl;

import com.liangshou.service.IEdictTasksService;
import com.liangshou.service.dto.EdictTasksDTO;
import com.liangshou.service.vo.EdictTasksVO;
import com.liangshou.infrastructure.datasource.po.EdictTasksPO;
import com.liangshou.infrastructure.datasource.support.IEdictTasksSupport;
import com.liangshou.common.utils.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EdictTasksServiceImpl implements IEdictTasksService {

    private final IEdictTasksSupport support;

    @Override
    public EdictTasksVO getById(String taskId) {
        EdictTasksPO po = support.getById(taskId);
        if (po == null) return null;
        EdictTasksVO vo = new EdictTasksVO();
        vo.setTaskId(po.getTaskId());
        return vo;
    }

    @Override
    public PageResult<EdictTasksVO> page(int current, int size) {
        Page<EdictTasksPO> page = support.page(new Page<>(current, size));
        return PageResult.of(
            page.getTotal(),
            page.getRecords().stream().map(po -> {
                EdictTasksVO vo = new EdictTasksVO();
                vo.setTaskId(po.getTaskId());
                return vo;
            }).collect(Collectors.toList()),
            page.getCurrent(),
            page.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EdictTasksDTO dto) {
        EdictTasksPO po = new EdictTasksPO();
        po.setTaskId(dto.getTaskId());
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(EdictTasksDTO dto) {
        EdictTasksPO po = new EdictTasksPO();
        po.setTaskId(dto.getTaskId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String taskId) {
        return support.removeById(taskId);
    }
}
