package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.repository.AgentStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 状态管理服务，负责更新和查询 Agent 的实时工作状态。
 */
@Service
public class AgentStatusService {

    private final AgentStatusRepository agentStatusRepository;

    /**
     * 构造函数。
     *
     * @param agentStatusRepository Agent 状态仓库
     */
    public AgentStatusService(AgentStatusRepository agentStatusRepository) {
        this.agentStatusRepository = agentStatusRepository;
    }

    /**
     * 更新指定会话的 Agent 状态。
     *
     * @param sessionId   会话 ID
     * @param status      新状态（WORKING / DONE / WAITING / IDLE）
     * @param currentTool 当前执行的工具名称，可为 null
     */
    public void updateStatus(String sessionId, String status, String currentTool) {
        AgentStatus agentStatus = agentStatusRepository.findById(sessionId)
                .orElse(AgentStatus.builder()
                        .sessionId(sessionId)
                        .build());
        agentStatus.setStatus(status);
        agentStatus.setCurrentTool(currentTool);
        agentStatus.setLastUpdatedAt(LocalDateTime.now());
        agentStatusRepository.save(agentStatus);
    }

    /**
     * 查询所有活跃的 Agent 状态（非 IDLE）。
     *
     * @return 活跃 Agent 状态列表
     */
    public List<AgentStatus> getActiveStatuses() {
        return agentStatusRepository.findAll().stream()
                .filter(s -> !"IDLE".equals(s.getStatus()))
                .toList();
    }
}
