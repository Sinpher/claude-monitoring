package com.sinpher.claudemonitor.websocket;

import com.sinpher.claudemonitor.dto.AgentStatusDto;
import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.service.AgentStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 状态 WebSocket 处理器，向前端推送实时状态变更。
 * 当有客户端连接时，定时推送当前所有活跃 Agent 的状态。
 */
@Slf4j
@Component
public class AgentStatusHandler extends TextWebSocketHandler {

    private final AgentStatusService agentStatusService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 当前连接的 WebSocket 会话列表 */
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    /**
     * 构造函数。
     *
     * @param agentStatusService Agent 状态服务
     */
    public AgentStatusHandler(AgentStatusService agentStatusService) {
        this.agentStatusService = agentStatusService;
    }

    /**
     * 新连接建立时，将 session 加入列表。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket 连接建立: {}", session.getId());
    }

    /**
     * 连接关闭时，移除 session。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("WebSocket 连接关闭: {}", session.getId());
    }

    /**
     * 定时推送 Agent 状态（每 2 秒）。
     * 向所有已连接的客户端广播当前活跃 Agent 状态。
     */
    @Scheduled(fixedRate = 2000)
    public void broadcastAgentStatus() {
        if (sessions.isEmpty()) {
            return;
        }

        List<AgentStatusDto> statuses = agentStatusService.getActiveStatuses().stream()
                .map(this::toDto)
                .toList();

        try {
            String json = objectMapper.writeValueAsString(statuses);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.error("推送 Agent 状态失败: {}", e.getMessage());
        }
    }

    /**
     * 将 AgentStatus 实体转换为 DTO。
     *
     * @param status AgentStatus 实体
     * @return AgentStatusDto
     */
    private AgentStatusDto toDto(AgentStatus status) {
        return AgentStatusDto.builder()
                .sessionId(status.getSessionId())
                .status(status.getStatus())
                .currentTool(status.getCurrentTool())
                .lastUpdatedAt(status.getLastUpdatedAt())
                .build();
    }
}
