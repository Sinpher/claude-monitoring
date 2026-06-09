package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.DailyUsageDto;
import com.sinpher.claudemonitor.service.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 用量统计 API 控制器，提供日用量和今日概览查询。
 */
@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

    /**
     * 构造函数。
     *
     * @param usageService 用量统计服务
     */
    public UsageController(UsageService usageService) {
        this.usageService = usageService;
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
}
