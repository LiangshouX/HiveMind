package com.liangshou.adapter.controller;

import com.liangshou.service.ISysTokenUsageService;
import com.liangshou.service.vo.SysTokenUsageVO;
import com.liangshou.common.utils.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SysTokenUsageController.class)
class SysTokenUsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISysTokenUsageService service;

    @Test
    @WithMockUser(username = "admin", roles = {"USER", "ADMIN"})
    void testGetById() throws Exception {
        SysTokenUsageVO vo = new SysTokenUsageVO();
        vo.setId(1L);
        when(service.getById(any())).thenReturn(vo);

        mockMvc.perform(get("/api/sys-token-usages/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
