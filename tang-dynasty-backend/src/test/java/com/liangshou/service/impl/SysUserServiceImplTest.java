package com.liangshou.service.impl;

import com.liangshou.infrastructure.datasource.po.SysUserPO;
import com.liangshou.infrastructure.datasource.support.ISysUserSupport;
import com.liangshou.service.vo.SysUserVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private ISysUserSupport support;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SysUserServiceImpl service;

    @Test
    void testGetById() {
        SysUserPO po = new SysUserPO();
        po.setId(1L);
        po.setUserId("user001");
        po.setNickname("测试用户");
        when(support.getById(any())).thenReturn(po);

        SysUserVO vo = service.getById(1L);
        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("user001", vo.getUserId());
        assertEquals("测试用户", vo.getNickname());
    }
}
