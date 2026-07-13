package com.liangshou.adapter.controller;

import com.liangshou.service.IAgentTaskService;
import com.liangshou.service.vo.AgentTaskVO;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgentTaskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IAgentTaskService service;

    @InjectMocks
    private AgentTaskController controller;

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
        AgentTaskVO vo = new AgentTaskVO();
        vo.setTaskId("test-id");
        when(service.getById(anyString(), anyString())).thenReturn(vo);

        mockMvc.perform(get("/api/agent-tasks/{taskId}", "test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("HME_SYSTEM_000"));
    }
}
