package com.sinpher.claudemonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token 用量记录实体，记录每条 assistant 消息的 token 用量，支持按天聚合统计。
 *
 * @author sinpher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "token_usage")
public class TokenUsage {

    /** 自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属会话 ID */
    @Column(length = 64, nullable = false)
    private String sessionId;

    /** 输入 token 数量 */
    @Column(nullable = false)
    private Long inputTokens;

    /** 输出 token 数量 */
    @Column(nullable = false)
    private Long outputTokens;

    /** 缓存 token 数量（含 creation + read） */
    @Column(nullable = false)
    private Long cacheTokens;

    /** 消息时间戳，用于按天聚合 */
    @Column(nullable = false)
    private LocalDateTime timestamp;
}