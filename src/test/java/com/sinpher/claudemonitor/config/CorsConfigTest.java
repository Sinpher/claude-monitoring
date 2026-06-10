package com.sinpher.claudemonitor.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS 配置测试，验证跨域请求头正确返回。
 */
@WebMvcTest(CorsConfig.class)
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试 API 路径的 CORS 预检请求返回正确的跨域头。
     */
    @Test
    @DisplayName("CORS 预检请求应返回允许的跨域头")
    void corsPreFlightShouldReturnAllowedHeaders() throws Exception {
        // 验证 OPTIONS 请求返回 200 且包含 CORS 头
        mockMvc.perform(options("/api/sessions")
                        .header("Origin", "http://localhost:1420")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}