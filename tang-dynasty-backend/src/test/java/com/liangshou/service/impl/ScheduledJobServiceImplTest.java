package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.ScheduledJobPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobSupport;
import com.liangshou.service.vo.ScheduledJobVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceImplTest {

    @Mock
    private IScheduledJobSupport support;

    @InjectMocks
    private ScheduledJobServiceImpl service;

    @Test
    void testGetById() {
        ScheduledJobPO po = new ScheduledJobPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        ScheduledJobVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
