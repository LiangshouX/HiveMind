package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.EdictTasksPO;
import com.liangshou.infrastructure.datasource.support.IEdictTasksSupport;
import com.liangshou.service.vo.EdictTasksVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdictTasksServiceImplTest {

    @Mock
    private IEdictTasksSupport support;

    @InjectMocks
    private EdictTasksServiceImpl service;

    @Test
    void testGetById() {
        EdictTasksPO po = new EdictTasksPO();
        po.setTaskId("test-id");
        when(support.getById(any())).thenReturn(po);

        EdictTasksVO vo = service.getById("test-id");
        assertNotNull(vo);
        assertEquals("test-id", vo.getTaskId());
    }
}
