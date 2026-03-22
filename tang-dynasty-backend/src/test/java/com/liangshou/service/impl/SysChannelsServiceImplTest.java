package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.SysChannelsPO;
import com.liangshou.infrastructure.datasource.support.ISysChannelsSupport;
import com.liangshou.service.vo.SysChannelsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysChannelsServiceImplTest {

    @Mock
    private ISysChannelsSupport support;

    @InjectMocks
    private SysChannelsServiceImpl service;

    @Test
    void testGetById() {
        SysChannelsPO po = new SysChannelsPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        SysChannelsVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
