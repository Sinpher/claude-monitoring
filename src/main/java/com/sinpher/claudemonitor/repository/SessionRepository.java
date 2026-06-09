package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话数据访问层，提供会话的 CRUD 和统计查询。
 *
 * @author sinpher
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    /**
     * 查询指定时间范围内的会话列表，按开始时间降序排列。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 会话列表
     */
    List<Session> findByStartedAtBetweenOrderByStartedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * 查询状态为 ACTIVE 的会话列表。
     *
     * @param status 状态值
     * @return 活跃会话列表
     */
    List<Session> findByStatus(String status);

    /**
     * 统计指定日期范围内的会话数量。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 会话数量
     */
    long countByStartedAtBetween(LocalDateTime start, LocalDateTime end);
}
