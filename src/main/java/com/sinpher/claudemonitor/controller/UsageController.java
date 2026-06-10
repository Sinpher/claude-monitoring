package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.DailyUsageDto;
import com.sinpher.claudemonitor.dto.ToolStatsDto;
import com.sinpher.claudemonitor.repository.ToolCallRepository;
import com.sinpher.claudemonitor.service.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用量统计 API 控制器，提供日用量、今日概览和工具统计查询。
 */
@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;
    private final ToolCallRepository toolCallRepository;

    /**
     * 构造函数。
     *
     * @param usageService       用量统计服务
     * @param toolCallRepository 工具调用仓库
     */
    public UsageController(UsageService usageService, ToolCallRepository toolCallRepository) {
        this.usageService = usageService;
        this.toolCallRepository = toolCallRepository;
    }

    /**
     * 查询日用量统计。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日用量 DTO 列表
     */
    @GetMapping("/daily")
    public ResponseEntity<List<DailyUsageDto>> getDailyUsage(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        return ResponseEntity.ok(usageService.getDailyUsage(start, end));
    }

    /**
     * 查询今日用量概览。
     *
     * @return 今日用量 DTO
     */
    @GetMapping("/today")
    public ResponseEntity<DailyUsageDto> getTodayUsage() {
        return ResponseEntity.ok(usageService.getTodayUsage());
    }

    /**
     * 查询工具调用统计（按工具名称聚合频率和平均耗时）。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 工具统计 DTO 列表
     */
    @GetMapping("/tools")
    public ResponseEntity<List<ToolStatsDto>> getToolStats(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        List<Object[]> counts = toolCallRepository.countByToolNameBetween(startDateTime, endDateTime);
        List<Object[]> durations = toolCallRepository.avgDurationByToolNameBetween(startDateTime, endDateTime);

        // 合并频率和耗时数据
        Map<String, Double> durationMap = new HashMap<>();
        for (Object[] row : durations) {
            durationMap.put((String) row[0], ((Number) row[1]).doubleValue());
        }

        List<ToolStatsDto> stats = counts.stream()
                .map(row -> new ToolStatsDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        durationMap.getOrDefault((String) row[0], 0.0)
                ))
                .toList();

        return ResponseEntity.ok(stats);
    }
}
