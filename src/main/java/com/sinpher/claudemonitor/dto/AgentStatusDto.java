package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 实时状态数据传输对象，用于 API 和 WebSocket 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatusDto {
    private String sessionId;
    private String status;
    private String currentTool;
    private LocalDateTime lastUpdatedAt;
}
