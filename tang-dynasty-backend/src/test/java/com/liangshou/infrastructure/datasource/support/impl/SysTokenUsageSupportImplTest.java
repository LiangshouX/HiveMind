package com.liangshou.infrastructure.datasource.support.impl;

import com.liangshou.infrastructure.datasource.mapper.SysTokenUsageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SysTokenUsageSupportImplTest {

    @Mock
    private SysTokenUsageMapper mapper;

    @InjectMocks
    private SysTokenUsageSupportImpl support;

    @Test
    void testMapper() {
        assertNotNull(mapper);
        assertNotNull(support);
    }
}
