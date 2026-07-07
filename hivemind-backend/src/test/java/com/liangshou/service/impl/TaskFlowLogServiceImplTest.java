package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.TaskFlowLogPO;
import com.liangshou.infrastructure.datasource.support.ITaskFlowLogSupport;
import com.liangshou.service.vo.TaskFlowLogVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskFlowLogServiceImplTest {

    @Mock
    private ITaskFlowLogSupport support;

    @InjectMocks
    private TaskFlowLogServiceImpl service;

    @Test
    void testGetById() {
        TaskFlowLogPO po = new TaskFlowLogPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        TaskFlowLogVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
