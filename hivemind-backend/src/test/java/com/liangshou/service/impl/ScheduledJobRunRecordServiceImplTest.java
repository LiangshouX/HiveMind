package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.liangshou.infrastructure.datasource.po.ScheduledJobRunRecordPO;
import com.liangshou.infrastructure.datasource.support.IScheduledJobRunRecordSupport;
import com.liangshou.service.vo.ScheduledJobRunRecordVO;
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
class ScheduledJobRunRecordServiceImplTest {

    @Mock
    private IScheduledJobRunRecordSupport support;

    @InjectMocks
    private ScheduledJobRunRecordServiceImpl service;

    @SuppressWarnings("unchecked")
    @Test
    void testGetById() {
        ScheduledJobRunRecordPO po = new ScheduledJobRunRecordPO();
        po.setId(1L);

        LambdaQueryChainWrapper<ScheduledJobRunRecordPO> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(support).lambdaQuery();
        doReturn(chain).when(chain).eq(any(), any());
        doReturn(po).when(chain).one();

        ScheduledJobRunRecordVO vo = service.getById("1", 1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
