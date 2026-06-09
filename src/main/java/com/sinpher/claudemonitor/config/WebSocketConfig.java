package com.sinpher.claudemonitor.config;

import com.sinpher.claudemonitor.websocket.AgentStatusHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类，注册 Agent 状态实时推送端点。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AgentStatusHandler agentStatusHandler;

    /**
     * 构造函数。
     *
     * @param agentStatusHandler Agent 状态 WebSocket 处理器
     */
    public WebSocketConfig(AgentStatusHandler agentStatusHandler) {
        this.agentStatusHandler = agentStatusHandler;
    }

    /**
     * 注册 WebSocket 端点。
     * 前端通过 ws://localhost:8080/ws/agent-status 连接获取实时状态。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentStatusHandler, "/ws/agent-status")
                .setAllowedOrigins("*");
    }
}
