package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会话详情数据传输对象，用于 API 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailDto {
    private String id;
    private String project;
    private String model;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Long totalCacheTokens;
    private BigDecimal estimatedCost;
    private Integer toolCallCount;
    private String status;
}
