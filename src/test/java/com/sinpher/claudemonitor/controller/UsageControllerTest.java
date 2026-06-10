package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.repository.ToolCallRepository;
import com.sinpher.claudemonitor.service.UsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UsageController 测试，验证工具统计 API 返回正确数据。
 */
@WebMvcTest(UsageController.class)
class UsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsageService usageService;

    @MockBean
    private ToolCallRepository toolCallRepository;

    /**
     * 测试工具统计 API 返回按名称聚合的频率数据。
     */
    @Test
    @DisplayName("工具统计 API 应返回按名称聚合的调用次数")
    void toolStatsShouldReturnCountByName() throws Exception {
        // 准备 mock 数据：Read 100 次，Edit 50 次
        when(toolCallRepository.countByToolNameBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{"Read", 100L}, new Object[]{"Edit", 50L}));
        when(toolCallRepository.avgDurationByToolNameBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // 验证 API 返回 200 且包含 2 个工具
        mockMvc.perform(get("/api/usage/tools")
                        .param("start", "2026-06-01")
                        .param("end", "2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].toolName").value("Read"))
                .andExpect(jsonPath("$[0].count").value(100));
    }

    /**
     * 测试工具统计 API 返回平均耗时数据。
     */
    @Test
    @DisplayName("工具统计 API 应返回各工具的平均耗时")
    void toolStatsShouldReturnAvgDuration() throws Exception {
        // 准备 mock 数据：Read 平均 500ms，Edit 平均 200ms
        when(toolCallRepository.countByToolNameBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{"Read", 100L}, new Object[]{"Edit", 50L}));
        when(toolCallRepository.avgDurationByToolNameBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Object[]{"Read", 500.0}, new Object[]{"Edit", 200.0}));

        // 验证 API 返回正确的平均耗时
        mockMvc.perform(get("/api/usage/tools")
                        .param("start", "2026-06-01")
                        .param("end", "2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].avgDuration").value(500.0))
                .andExpect(jsonPath("$[1].avgDuration").value(200.0));
    }

    /**
     * 测试工具统计 API 在无数据时返回空列表。
     */
    @Test
    @DisplayName("工具统计 API 在无数据时应返回空列表")
    void toolStatsShouldReturnEmptyListWhenNoData() throws Exception {
        // 准备 mock 数据：无工具调用
        when(toolCallRepository.countByToolNameBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(toolCallRepository.avgDurationByToolNameBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // 验证 API 返回空列表
        mockMvc.perform(get("/api/usage/tools")
                        .param("start", "2026-06-01")
                        .param("end", "2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
