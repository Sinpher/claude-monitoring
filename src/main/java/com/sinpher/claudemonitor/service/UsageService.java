package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.dto.DailyUsageDto;
import com.sinpher.claudemonitor.model.DailyUsage;
import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.repository.DailyUsageRepository;
import com.sinpher.claudemonitor.repository.SessionRepository;
import com.sinpher.claudemonitor.repository.ToolCallRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 用量统计服务，提供日用量和月用量的聚合查询。
 */
@Service
public class UsageService {

    private final SessionRepository sessionRepository;
    private final DailyUsageRepository dailyUsageRepository;
    private final ToolCallRepository toolCallRepository;

    /**
     * 构造函数。
     *
     * @param sessionRepository    会话仓库
     * @param dailyUsageRepository 日用量仓库
     * @param toolCallRepository   工具调用仓库
     */
    public UsageService(SessionRepository sessionRepository,
                        DailyUsageRepository dailyUsageRepository,
                        ToolCallRepository toolCallRepository) {
        this.sessionRepository = sessionRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.toolCallRepository = toolCallRepository;
    }

    /**
     * 查询指定日期范围内的日用量统计。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日用量 DTO 列表
     */
    public List<DailyUsageDto> getDailyUsage(LocalDate start, LocalDate end) {
        List<DailyUsage> usages = dailyUsageRepository.findByDateBetweenOrderByDateAsc(start, end);
        return usages.stream().map(this::toDailyUsageDto).toList();
    }

    /**
     * 获取今日用量概览。
     *
     * @return 今日用量 DTO
     */
    public DailyUsageDto getTodayUsage() {
        LocalDate today = LocalDate.now();
        List<Session> todaySessions = sessionRepository.findByStartedAtBetweenOrderByStartedAtDesc(
                today.atStartOfDay(), today.atTime(LocalTime.MAX));

        long totalInput = todaySessions.stream().mapToLong(Session::getTotalInputTokens).sum();
        long totalOutput = todaySessions.stream().mapToLong(Session::getTotalOutputTokens).sum();
        long totalCache = todaySessions.stream().mapToLong(Session::getTotalCacheTokens).sum();
        BigDecimal totalCost = todaySessions.stream()
                .map(Session::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long toolCalls = toolCallRepository.countByTimestampBetween(
                today.atStartOfDay(), today.atTime(LocalTime.MAX));

        return DailyUsageDto.builder()
                .date(today)
                .totalInputTokens(totalInput)
                .totalOutputTokens(totalOutput)
                .totalCacheTokens(totalCache)
                .estimatedCost(totalCost)
                .sessionCount(todaySessions.size())
                .toolCallCount((int) toolCalls)
                .build();
    }

    /**
     * 将 DailyUsage 实体转换为 DTO。
     *
     * @param usage 日用量实体
     * @return 日用量 DTO
     */
    private DailyUsageDto toDailyUsageDto(DailyUsage usage) {
        return DailyUsageDto.builder()
                .date(usage.getDate())
                .totalInputTokens(usage.getTotalInputTokens())
                .totalOutputTokens(usage.getTotalOutputTokens())
                .totalCacheTokens(usage.getTotalCacheTokens())
                .estimatedCost(usage.getEstimatedCost())
                .sessionCount(usage.getSessionCount())
                .toolCallCount(usage.getToolCallCount())
                .build();
    }
}
