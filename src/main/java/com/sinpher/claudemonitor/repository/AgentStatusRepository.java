package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Agent 实时状态数据访问层，提供状态的 CRUD 和查询。
 *
 * @author sinpher
 */
@Repository
public interface AgentStatusRepository extends JpaRepository<AgentStatus, String> {

    /**
     * 查询指定状态的 Agent 列表。
     *
     * @param status 状态值（WORKING / DONE / WAITING / IDLE）
     * @return Agent 状态列表
     */
    List<AgentStatus> findByStatus(String status);
}
