package com.liangshou.service.impl;

import com.liangshou.service.IEdictTasksService;
import com.liangshou.service.dto.EdictTasksDTO;
import com.liangshou.service.vo.EdictTasksVO;
import com.liangshou.infrastructure.datasource.po.EdictTasksPO;
import com.liangshou.infrastructure.datasource.support.IEdictTasksSupport;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
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
    public EdictTasksVO getById(String userId, String taskId) {
        EdictTasksPO po = support.lambdaQuery()
                .eq(EdictTasksPO::getTaskId, taskId)
                .eq(EdictTasksPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) return null;
        EdictTasksVO vo = new EdictTasksVO();
        vo.setTaskId(po.getTaskId());
        return vo;
    }

    @Override
    public PageResult<EdictTasksVO> page(String userId, int current, int size) {
        Page<EdictTasksPO> page = support.lambdaQuery()
                .eq(EdictTasksPO::getUserId, Long.valueOf(userId))
                .page(new Page<>(current, size));
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
    public boolean save(String userId, EdictTasksDTO dto) {
        EdictTasksPO po = new EdictTasksPO();
        po.setTaskId(dto.getTaskId());
        po.setUserId(Long.valueOf(userId));
        return support.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, EdictTasksDTO dto) {
        EdictTasksPO po = support.lambdaQuery()
                .eq(EdictTasksPO::getTaskId, dto.getTaskId())
                .eq(EdictTasksPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限修改");
        }
        po.setTaskId(dto.getTaskId());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String userId, String taskId) {
        EdictTasksPO po = support.lambdaQuery()
                .eq(EdictTasksPO::getTaskId, taskId)
                .eq(EdictTasksPO::getUserId, Long.valueOf(userId))
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        return support.removeById(taskId);
    }
}
