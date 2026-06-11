package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.TokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Token 用量数据访问层，提供 TokenUsage 的 CRUD 和按天聚合查询。
 *
 * @author sinpher
 */
@Repository
public interface TokenUsageRepository extends JpaRepository<TokenUsage, Long> {

    /**
     * 查询指定时间范围内的 Token 用量记录，用于 Java 侧按天聚合。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return TokenUsage 列表
     */
    List<TokenUsage> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计指定时间范围内的活跃会话数（去重）。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 活跃会话数
     */
    @Query("SELECT COUNT(DISTINCT tu.sessionId) FROM TokenUsage tu WHERE tu.timestamp BETWEEN :start AND :end")
    long countDistinctSessionIdByTimestampBetween(LocalDateTime start, LocalDateTime end);
}