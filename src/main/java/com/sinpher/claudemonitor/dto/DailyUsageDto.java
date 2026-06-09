package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日用量数据传输对象，用于 API 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageDto {
    private LocalDate date;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Long totalCacheTokens;
    private BigDecimal estimatedCost;
    private Integer sessionCount;
    private Integer toolCallCount;
}
