package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.AgentStatusDto;
import com.sinpher.claudemonitor.dto.HookEventDto;
import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.service.AgentStatusService;
import com.sinpher.claudemonitor.service.HookEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Hook 事件接收控制器，接收 Claude Code Hooks 推送的事件。
 */
@RestController
@RequestMapping("/api/hooks")
public class HookEventController {

    private final HookEventService hookEventService;
    private final AgentStatusService agentStatusService;

    /**
     * 构造函数。
     *
     * @param hookEventService   Hook 事件处理服务
     * @param agentStatusService Agent 状态服务
     */
    public HookEventController(HookEventService hookEventService, AgentStatusService agentStatusService) {
        this.hookEventService = hookEventService;
        this.agentStatusService = agentStatusService;
    }

    /**
     * 接收 Hook 事件并处理。
     *
     * @param event Hook 事件 DTO
     * @return 空响应，成功时返回 200
     */
    @PostMapping("/event")
    public ResponseEntity<Void> receiveHookEvent(@RequestBody HookEventDto event) {
        hookEventService.processHookEvent(event);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询当前活跃的 Agent 状态列表。
     *
     * @return 活跃 Agent 状态列表
     */
    @GetMapping("/status")
    public ResponseEntity<List<AgentStatusDto>> getActiveStatuses() {
        List<AgentStatusDto> statuses = agentStatusService.getActiveStatuses().stream()
                .map(s -> AgentStatusDto.builder()
                        .sessionId(s.getSessionId())
                        .status(s.getStatus())
                        .currentTool(s.getCurrentTool())
                        .lastUpdatedAt(s.getLastUpdatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(statuses);
    }
}
