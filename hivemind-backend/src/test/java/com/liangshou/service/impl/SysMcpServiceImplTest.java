package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.SysMcpPO;
import com.liangshou.infrastructure.datasource.support.ISysMcpSupport;
import com.liangshou.service.vo.SysMcpVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysMcpServiceImplTest {

    @Mock
    private ISysMcpSupport support;

    @InjectMocks
    private SysMcpServiceImpl service;

    @Test
    void testGetById() {
        SysMcpPO po = new SysMcpPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        SysMcpVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
