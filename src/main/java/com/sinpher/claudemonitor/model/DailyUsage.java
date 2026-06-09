package com.sinpher.claudemonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日用量汇总实体，按日期聚合 token 用量和费用。
 *
 * @author sinpher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_usage")
public class DailyUsage {

    /** 日期（主键） */
    @Id
    private LocalDate date;

    /** 输入 token 总量 */
    @Builder.Default
    @Column(nullable = false)
    private Long totalInputTokens = 0L;

    /** 输出 token 总量 */
    @Builder.Default
    @Column(nullable = false)
    private Long totalOutputTokens = 0L;

    /** 缓存 token 总量 */
    @Builder.Default
    @Column(nullable = false)
    private Long totalCacheTokens = 0L;

    /** 预估费用（USD） */
    @Builder.Default
    @Column(precision = 10, scale = 6)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    /** 会话数 */
    @Builder.Default
    @Column(nullable = false)
    private Integer sessionCount = 0;

    /** 工具调用次数 */
    @Builder.Default
    @Column(nullable = false)
    private Integer toolCallCount = 0;
}
