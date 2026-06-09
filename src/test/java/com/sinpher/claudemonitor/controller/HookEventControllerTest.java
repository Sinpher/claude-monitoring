package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.HookEventDto;
import com.sinpher.claudemonitor.service.AgentStatusService;
import com.sinpher.claudemonitor.service.HookEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HookEventController 测试，验证 Hook 事件接收端点。
 */
@WebMvcTest(HookEventController.class)
class HookEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HookEventService hookEventService;

    @MockBean
    private AgentStatusService agentStatusService;

    @Test
    @DisplayName("POST /api/hooks/event 应返回 200 并调用处理服务")
    void shouldReceiveHookEvent() throws Exception {
        // 准备测试数据
        String json = """
                {"hookEvent":"UserPromptSubmit","sessionId":"test-001","timestamp":"2026-06-09T10:00:00Z"}
                """;

        // 执行请求并验证响应状态
        mockMvc.perform(post("/api/hooks/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // 验证服务层被调用
        verify(hookEventService).processHookEvent(any(HookEventDto.class));
    }
}
