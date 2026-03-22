package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.EdictMemorialPO;
import com.liangshou.infrastructure.datasource.support.IEdictMemorialSupport;
import com.liangshou.service.vo.EdictMemorialVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdictMemorialServiceImplTest {

    @Mock
    private IEdictMemorialSupport support;

    @InjectMocks
    private EdictMemorialServiceImpl service;

    @Test
    void testGetById() {
        EdictMemorialPO po = new EdictMemorialPO();
        po.setId(1L);
        when(support.getById(any())).thenReturn(po);

        EdictMemorialVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
