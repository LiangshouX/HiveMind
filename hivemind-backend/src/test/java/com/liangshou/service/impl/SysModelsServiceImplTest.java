package com.liangshou.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.liangshou.infrastructure.datasource.po.SysModelsPO;
import com.liangshou.infrastructure.datasource.support.ISysModelsSupport;
import com.liangshou.service.vo.SysModelsVO;
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
class SysModelsServiceImplTest {

    @Mock
    private ISysModelsSupport support;

    @InjectMocks
    private SysModelsServiceImpl service;

    @SuppressWarnings("unchecked")
    @Test
    void testGetById() {
        SysModelsPO po = new SysModelsPO();
        po.setId(1L);

        LambdaQueryChainWrapper<SysModelsPO> chain = mock(LambdaQueryChainWrapper.class);
        doReturn(chain).when(support).lambdaQuery();
        doReturn(chain).when(chain).eq(any(), any());
        doReturn(po).when(chain).one();

        SysModelsVO vo = service.getById("1", 1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
    }
}
