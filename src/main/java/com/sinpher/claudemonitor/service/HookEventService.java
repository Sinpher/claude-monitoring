package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.dto.HookEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Hook 事件处理服务，将 Claude Code 的 Hook 事件映射为 Agent 状态变更。
 *
 * 事件到状态的映射规则：
 * - UserPromptSubmit → WORKING
 * - PreToolUse → WAITING
 * - PostToolUse → WORKING（记录当前工具）
 * - Stop → DONE
 */
@Slf4j
@Service
public class HookEventService {

    private final AgentStatusService agentStatusService;

    /**
     * 构造函数。
     *
     * @param agentStatusService Agent 状态服务
     */
    public HookEventService(AgentStatusService agentStatusService) {
        this.agentStatusService = agentStatusService;
    }

    /**
     * 处理接收到的 Hook 事件，更新 Agent 状态。
     *
     * @param event Hook 事件 DTO
     */
    public void processHookEvent(HookEventDto event) {
        String sessionId = event.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("收到缺少 sessionId 的 Hook 事件: {}", event.getHookEvent());
            return;
        }

        String status = mapEventToStatus(event.getHookEvent());
        String currentTool = extractCurrentTool(event);

        agentStatusService.updateStatus(sessionId, status, currentTool);
        log.debug("Hook 事件处理完成: event={}, sessionId={}, status={}",
                event.getHookEvent(), sessionId, status);
    }

    /**
     * 将 Hook 事件类型映射为 Agent 状态。
     *
     * @param hookEvent Hook 事件类型
     * @return 对应的 Agent 状态
     */
    private String mapEventToStatus(String hookEvent) {
        return switch (hookEvent) {
            case "UserPromptSubmit" -> "WORKING";
            case "PreToolUse" -> "WAITING";
            case "PostToolUse" -> "WORKING";
            case "Stop" -> "DONE";
            default -> "IDLE";
        };
    }

    /**
     * 从 Hook 事件中提取当前工具名称。
     *
     * @param event Hook 事件 DTO
     * @return 工具名称，无工具时返回 null
     */
    private String extractCurrentTool(HookEventDto event) {
        if ("PostToolUse".equals(event.getHookEvent()) || "PreToolUse".equals(event.getHookEvent())) {
            return event.getToolName();
        }
        return null;
    }
}
