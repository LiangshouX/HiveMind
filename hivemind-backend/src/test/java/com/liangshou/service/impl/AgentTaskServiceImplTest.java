package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.AgentTaskPO;
import com.liangshou.infrastructure.datasource.support.IAgentTaskSupport;
import com.liangshou.service.vo.AgentTaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentTaskServiceImplTest {

    @Mock
    private IAgentTaskSupport support;

    @InjectMocks
    private AgentTaskServiceImpl service;

    @Test
    void testGetById() {
        AgentTaskPO po = new AgentTaskPO();
        po.setTaskId("test-id");
        when(support.getById(any())).thenReturn(po);

        AgentTaskVO vo = service.getById("test-id");
        assertNotNull(vo);
        assertEquals("test-id", vo.getTaskId());
    }
}
