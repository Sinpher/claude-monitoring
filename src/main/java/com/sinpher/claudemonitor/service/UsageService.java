package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.dto.DailyUsageDto;
import com.sinpher.claudemonitor.model.DailyUsage;
import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.model.TokenUsage;
import com.sinpher.claudemonitor.repository.DailyUsageRepository;
import com.sinpher.claudemonitor.repository.SessionRepository;
import com.sinpher.claudemonitor.repository.TokenUsageRepository;
import com.sinpher.claudemonitor.repository.ToolCallRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用量统计服务，提供日用量和月用量的聚合查询。
 */
@Service
public class UsageService {

    private final SessionRepository sessionRepository;
    private final DailyUsageRepository dailyUsageRepository;
    private final ToolCallRepository toolCallRepository;
    private final TokenUsageRepository tokenUsageRepository;
    private final CostCalculationService costCalculationService;

    /**
     * 构造函数。
     *
     * @param sessionRepository      会话仓库
     * @param dailyUsageRepository   日用量仓库
     * @param toolCallRepository     工具调用仓库
     * @param tokenUsageRepository   Token 用量仓库
     * @param costCalculationService 成本计算服务
     */
    public UsageService(SessionRepository sessionRepository,
                        DailyUsageRepository dailyUsageRepository,
                        ToolCallRepository toolCallRepository,
                        TokenUsageRepository tokenUsageRepository,
                        CostCalculationService costCalculationService) {
        this.sessionRepository = sessionRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.toolCallRepository = toolCallRepository;
        this.tokenUsageRepository = tokenUsageRepository;
        this.costCalculationService = costCalculationService;
    }

    /**
     * 查询指定日期范围内的日用量统计，从 TokenUsage 表按 timestamp 在 Java 侧按天聚合。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日用量 DTO 列表
     */
    public List<DailyUsageDto> getDailyUsage(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        // 查询时间范围内的所有 TokenUsage 记录
        List<TokenUsage> usages = tokenUsageRepository.findByTimestampBetween(startDateTime, endDateTime);

        // 按天聚合
        Map<LocalDate, List<TokenUsage>> grouped = usages.stream()
                .collect(Collectors.groupingBy(tu -> tu.getTimestamp().toLocalDate()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<TokenUsage> dayUsages = entry.getValue();

                    long totalInput = dayUsages.stream().mapToLong(TokenUsage::getInputTokens).sum();
                    long totalOutput = dayUsages.stream().mapToLong(TokenUsage::getOutputTokens).sum();
                    long totalCache = dayUsages.stream().mapToLong(TokenUsage::getCacheTokens).sum();
                    int sessionCount = (int) dayUsages.stream().map(TokenUsage::getSessionId).distinct().count();

                    long toolCalls = toolCallRepository.countByTimestampBetween(
                            date.atStartOfDay(), date.atTime(LocalTime.MAX));

                    BigDecimal totalCost = calculateDailyCost(date, totalInput, totalOutput, totalCache);

                    return DailyUsageDto.builder()
                            .date(date)
                            .totalInputTokens(totalInput)
                            .totalOutputTokens(totalOutput)
                            .totalCacheTokens(totalCache)
                            .estimatedCost(totalCost)
                            .sessionCount(sessionCount)
                            .toolCallCount((int) toolCalls)
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }

    /**
     * 获取今日用量概览，从 TokenUsage 表按 timestamp 聚合。
     *
     * @return 今日用量 DTO
     */
    public DailyUsageDto getTodayUsage() {
        LocalDate today = LocalDate.now();
        List<DailyUsageDto> dailyList = getDailyUsage(today, today);
        if (dailyList.isEmpty()) {
            long toolCalls = toolCallRepository.countByTimestampBetween(
                    today.atStartOfDay(), today.atTime(LocalTime.MAX));
            return DailyUsageDto.builder()
                    .date(today)
                    .totalInputTokens(0L)
                    .totalOutputTokens(0L)
                    .totalCacheTokens(0L)
                    .estimatedCost(BigDecimal.ZERO)
                    .sessionCount(0)
                    .toolCallCount((int) toolCalls)
                    .build();
        }
        return dailyList.getFirst();
    }

    /**
     * 计算指定日期的 token 用量费用，取当天活跃会话的模型。
     *
     * @param date          日期
     * @param totalInput    输入 token 总量
     * @param totalOutput   输出 token 总量
     * @param totalCache    缓存 token 总量
     * @return 预估费用
     */
    private BigDecimal calculateDailyCost(LocalDate date, long totalInput, long totalOutput, long totalCache) {
        // 查询当天有活动的会话，取第一个有模型的会话的模型名
        List<Session> daySessions = sessionRepository.findByStartedAtBetweenOrderByStartedAtDesc(
                date.minusDays(30).atStartOfDay(), date.atTime(LocalTime.MAX));
        String model = daySessions.stream()
                .filter(s -> s.getModel() != null)
                .map(Session::getModel)
                .findFirst()
                .orElse(null);

        return costCalculationService.calculateCost(model, totalInput, totalOutput, totalCache, 0L);
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
