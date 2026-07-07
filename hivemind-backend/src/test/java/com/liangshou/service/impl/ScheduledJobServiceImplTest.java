package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceImplTest {

    @Mock
    private IScheduledJobSupport support;

    @InjectMocks
    private ScheduledJobServiceImpl service;

    @SuppressWarnings("unchecked")
    @Test
    void testGetById() {
        ScheduledJobPO po = new ScheduledJobPO();
        po.setId(1L);

        LambdaQueryChainWrapper<ScheduledJobPO> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(support).lambdaQuery();
        doReturn(chain).when(chain).eq(any(), any());
        doReturn(po).when(chain).one();

        ScheduledJobVO vo = service.getById("1", 1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
