package com.sinpher.claudemonitor.dto;

/**
 * 工具调用统计 DTO，包含工具名称、调用次数和平均耗时。
 */
public record ToolStatsDto(
        /** 工具名称 */
        String toolName,
        /** 调用次数 */
        Long count,
        /** 平均耗时（毫秒） */
        Double avgDuration
) {
}
