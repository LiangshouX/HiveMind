package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.SysTokenUsagePO;
import com.liangshou.infrastructure.datasource.support.ISysTokenUsageSupport;
import com.liangshou.service.vo.SysTokenUsageVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysTokenUsageServiceImplTest {

    @Mock
    private ISysTokenUsageSupport support;

    @InjectMocks
    private SysTokenUsageServiceImpl service;

    @Test
    void testGetById() {
        SysTokenUsagePO po = new SysTokenUsagePO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        SysTokenUsageVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
