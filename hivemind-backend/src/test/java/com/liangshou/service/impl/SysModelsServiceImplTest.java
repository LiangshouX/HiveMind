package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.SysModelsPO;
import com.liangshou.infrastructure.datasource.support.ISysModelsSupport;
import com.liangshou.service.vo.SysModelsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysModelsServiceImplTest {

    @Mock
    private ISysModelsSupport support;

    @InjectMocks
    private SysModelsServiceImpl service;

    @Test
    void testGetById() {
        SysModelsPO po = new SysModelsPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        SysModelsVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
