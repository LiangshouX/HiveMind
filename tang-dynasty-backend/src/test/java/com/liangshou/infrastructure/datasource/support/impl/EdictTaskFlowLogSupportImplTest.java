package com.liangshou.infrastructure.datasource.support.impl;

import com.liangshou.infrastructure.datasource.mapper.EdictTaskFlowLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EdictTaskFlowLogSupportImplTest {

    @Mock
    private EdictTaskFlowLogMapper mapper;

    @InjectMocks
    private EdictTaskFlowLogSupportImpl support;

    @Test
    void testMapper() {
        assertNotNull(mapper);
        assertNotNull(support);
    }
}
