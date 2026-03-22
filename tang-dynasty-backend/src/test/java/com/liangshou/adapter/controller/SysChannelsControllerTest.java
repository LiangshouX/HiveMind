package com.liangshou.adapter.controller;

import com.liangshou.service.ISysChannelsService;
import com.liangshou.service.vo.SysChannelsVO;
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

@WebMvcTest(controllers = SysChannelsController.class)
class SysChannelsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISysChannelsService service;

    @Test
    @WithMockUser(username = "admin", roles = {"USER", "ADMIN"})
    void testGetById() throws Exception {
        SysChannelsVO vo = new SysChannelsVO();
        vo.setId(1L);
        when(service.getById(any())).thenReturn(vo);

        mockMvc.perform(get("/api/sys-channelss/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
