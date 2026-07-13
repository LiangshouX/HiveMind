package com.liangshou.adapter.controller;

import com.liangshou.service.ISysTokenUsageService;
import com.liangshou.service.vo.SysTokenUsageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysTokenUsageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ISysTokenUsageService service;

    @InjectMocks
    private SysTokenUsageController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetById() throws Exception {
        SysTokenUsageVO vo = new SysTokenUsageVO();
        vo.setId(1L);
        when(service.getById(anyString(), anyLong())).thenReturn(vo);

        mockMvc.perform(get("/api/sys-token-usages/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HME_SYSTEM_000"));
    }
}
