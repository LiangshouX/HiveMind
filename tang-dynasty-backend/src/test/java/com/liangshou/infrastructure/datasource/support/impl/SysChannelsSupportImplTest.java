package com.liangshou.infrastructure.datasource.support.impl;

import com.liangshou.infrastructure.datasource.mapper.SysChannelsMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SysChannelsSupportImplTest {

    @Mock
    private SysChannelsMapper mapper;

    @InjectMocks
    private SysChannelsSupportImpl support;

    @Test
    void testMapper() {
        assertNotNull(mapper);
        assertNotNull(support);
    }
}
