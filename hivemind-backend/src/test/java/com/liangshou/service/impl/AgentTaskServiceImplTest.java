package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AgentTaskServiceImplTest {

    @Mock
    private IAgentTaskSupport support;

    @InjectMocks
    private AgentTaskServiceImpl service;

    @SuppressWarnings("unchecked")
    @Test
    void testGetById() {
        AgentTaskPO po = new AgentTaskPO();
        po.setTaskId("test-id");

        LambdaQueryChainWrapper<AgentTaskPO> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(support).lambdaQuery();
        doReturn(chain).when(chain).eq(any(), any());
        doReturn(po).when(chain).one();

        AgentTaskVO vo = service.getById("1", "test-id");
        assertNotNull(vo);
        assertEquals("test-id", vo.getTaskId());
    }
}
