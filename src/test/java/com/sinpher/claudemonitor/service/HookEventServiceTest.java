package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.dto.HookEventDto;
import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.repository.AgentStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HookEventService 单元测试，验证 Hook 事件到 Agent 状态的映射逻辑。
 */
class HookEventServiceTest {

    private AgentStatusRepository agentStatusRepository;
    private AgentStatusService agentStatusService;
    private HookEventService hookEventService;

    @BeforeEach
    void setUp() {
        agentStatusRepository = mock(AgentStatusRepository.class);
        agentStatusService = new AgentStatusService(agentStatusRepository);
        hookEventService = new HookEventService(agentStatusService);
    }

    @Test
    @DisplayName("UserPromptSubmit 事件应将 Agent 状态设为 WORKING")
    void shouldSetWorkingOnUserPromptSubmit() {
        // 测试数据准备：构造 UserPromptSubmit 事件
        HookEventDto event = HookEventDto.builder()
                .hookEvent("UserPromptSubmit")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:00:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        // 被测动作执行：处理 Hook 事件
        hookEventService.processHookEvent(event);

        // 结果验证：状态应为 WORKING，会话 ID 应一致
        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("UserPromptSubmit 事件应将状态设为 WORKING")
                .isEqualTo("WORKING");
        assertThat(captor.getValue().getSessionId())
                .as("会话 ID 应与事件一致")
                .isEqualTo("session-001");
    }

    @Test
    @DisplayName("Stop 事件应将 Agent 状态设为 DONE")
    void shouldSetDoneOnStop() {
        // 测试数据准备：构造 Stop 事件
        HookEventDto event = HookEventDto.builder()
                .hookEvent("Stop")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:05:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        // 被测动作执行：处理 Hook 事件
        hookEventService.processHookEvent(event);

        // 结果验证：状态应为 DONE
        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("Stop 事件应将状态设为 DONE")
                .isEqualTo("DONE");
    }

    @Test
    @DisplayName("PostToolUse 事件应保持 WORKING 状态并记录当前工具")
    void shouldKeepWorkingOnPostToolUse() {
        // 测试数据准备：构造 PostToolUse 事件，工具为 Edit
        HookEventDto event = HookEventDto.builder()
                .hookEvent("PostToolUse")
                .toolName("Edit")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:01:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        // 被测动作执行：处理 Hook 事件
        hookEventService.processHookEvent(event);

        // 结果验证：状态应为 WORKING，当前工具应为 Edit
        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("PostToolUse 事件应保持 WORKING 状态")
                .isEqualTo("WORKING");
        assertThat(captor.getValue().getCurrentTool())
                .as("当前工具应为 Edit")
                .isEqualTo("Edit");
    }

    @Test
    @DisplayName("PreToolUse 事件应将 Agent 状态设为 WAITING")
    void shouldSetWaitingOnPreToolUse() {
        // 测试数据准备：构造 PreToolUse 事件，工具为 Bash
        HookEventDto event = HookEventDto.builder()
                .hookEvent("PreToolUse")
                .toolName("Bash")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:02:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        // 被测动作执行：处理 Hook 事件
        hookEventService.processHookEvent(event);

        // 结果验证：状态应为 WAITING，当前工具应为 Bash
        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("PreToolUse 事件应将状态设为 WAITING")
                .isEqualTo("WAITING");
        assertThat(captor.getValue().getCurrentTool())
                .as("当前工具应为 Bash")
                .isEqualTo("Bash");
    }
}
