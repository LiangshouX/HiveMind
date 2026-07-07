package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.liangshou.infrastructure.datasource.po.TaskReportPO;
import com.liangshou.infrastructure.datasource.support.ITaskReportSupport;
import com.liangshou.service.vo.TaskReportVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TaskReportServiceImplTest {

    @Mock
    private ITaskReportSupport support;

    @InjectMocks
    private TaskReportServiceImpl service;

    @SuppressWarnings("unchecked")
    @Test
    void testGetById() {
        TaskReportPO po = new TaskReportPO();
        po.setId(1L);

        LambdaQueryChainWrapper<TaskReportPO> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(support).lambdaQuery();
        doReturn(chain).when(chain).eq(any(), any());
        doReturn(po).when(chain).one();

        TaskReportVO vo = service.getById("1", 1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
