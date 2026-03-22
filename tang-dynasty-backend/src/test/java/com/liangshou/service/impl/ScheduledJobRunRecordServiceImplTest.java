package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.ScheduledJobRunRecordPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobRunRecordSupport;
import com.liangshou.service.vo.ScheduledJobRunRecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledJobRunRecordServiceImplTest {

    @Mock
    private IScheduledJobRunRecordSupport support;

    @InjectMocks
    private ScheduledJobRunRecordServiceImpl service;

    @Test
    void testGetById() {
        ScheduledJobRunRecordPO po = new ScheduledJobRunRecordPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        ScheduledJobRunRecordVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
