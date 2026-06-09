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
import java.time.LocalDateTime;

/**
 * Claude Code 会话实体，记录每次会话的 token 用量和费用。
 *
 * @author sinpher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sessions")
public class Session {

    /** 会话唯一标识 */
    @Id
    @Column(length = 64)
    private String id;

    /** 所属项目名称 */
    @Column(length = 256)
    private String project;

    /** 使用的模型名称 */
    @Column(length = 128)
    private String model;

    /** 会话开始时间 */
    private LocalDateTime startedAt;

    /** 会话结束时间，进行中为 null */
    private LocalDateTime endedAt;

    /** 输入 token 总量 */
    @Builder.Default
    @Column(nullable = false)
    private Long totalInputTokens = 0L;

    /** 输出 token 总量 */
    @Builder.Default
    @Column(nullable = false)
    private Long totalOutputTokens = 0L;

    /** 缓存 token 总量（含 creation + read） */
    @Builder.Default
    @Column(nullable = false)
    private Long totalCacheTokens = 0L;

    /** 预估费用（USD） */
    @Builder.Default
    @Column(precision = 10, scale = 6)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    /** 工具调用次数 */
    @Builder.Default
    @Column(nullable = false)
    private Integer toolCallCount = 0;

    /** 会话状态：ACTIVE / COMPLETED */
    @Builder.Default
    @Column(length = 16, nullable = false)
    private String status = "ACTIVE";
}
