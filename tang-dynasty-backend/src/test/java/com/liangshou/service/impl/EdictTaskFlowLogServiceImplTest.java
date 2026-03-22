package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.EdictTaskFlowLogPO;
import com.liangshou.infrastructure.datasource.support.IEdictTaskFlowLogSupport;
import com.liangshou.service.vo.EdictTaskFlowLogVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdictTaskFlowLogServiceImplTest {

    @Mock
    private IEdictTaskFlowLogSupport support;

    @InjectMocks
    private EdictTaskFlowLogServiceImpl service;

    @Test
    void testGetById() {
        EdictTaskFlowLogPO po = new EdictTaskFlowLogPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        EdictTaskFlowLogVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
