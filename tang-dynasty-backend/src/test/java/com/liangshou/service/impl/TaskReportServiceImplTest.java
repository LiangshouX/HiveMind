package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.TaskReportPO;
import com.liangshou.infrastructure.datasource.support.ITaskReportSupport;
import com.liangshou.service.vo.TaskReportVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskReportServiceImplTest {

    @Mock
    private ITaskReportSupport support;

    @InjectMocks
    private TaskReportServiceImpl service;

    @Test
    void testGetById() {
        TaskReportPO po = new TaskReportPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        TaskReportVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
