package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.ToolCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具调用数据访问层，提供工具调用的 CRUD 和统计查询。
 *
 * @author sinpher
 */
@Repository
public interface ToolCallRepository extends JpaRepository<ToolCall, Long> {

    /**
     * 查询指定会话的工具调用列表，按时间升序排列。
     *
     * @param sessionId 会话 ID
     * @return 工具调用列表
     */
    List<ToolCall> findBySessionIdOrderByTimestampAsc(String sessionId);

    /**
     * 统计指定时间范围内各工具的调用次数。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 工具名和调用次数的投影列表
     */
    @Query("SELECT tc.toolName, COUNT(tc) FROM ToolCall tc WHERE tc.timestamp BETWEEN :start AND :end GROUP BY tc.toolName ORDER BY COUNT(tc) DESC")
    List<Object[]> countByToolNameBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计指定时间范围内的工具调用总数。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 调用总数
     */
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
