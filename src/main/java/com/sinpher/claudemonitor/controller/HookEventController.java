package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.HookEventDto;
import com.sinpher.claudemonitor.service.HookEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hook 事件接收控制器，接收 Claude Code Hooks 推送的事件。
 */
@RestController
@RequestMapping("/api/hooks")
public class HookEventController {

    private final HookEventService hookEventService;

    /**
     * 构造函数。
     *
     * @param hookEventService Hook 事件处理服务
     */
    public HookEventController(HookEventService hookEventService) {
        this.hookEventService = hookEventService;
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
}
