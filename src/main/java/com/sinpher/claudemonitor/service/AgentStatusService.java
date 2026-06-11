package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.repository.AgentStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 状态管理服务，负责更新和查询 Agent 的实时工作状态。
 * DONE 状态超过 30 秒自动过期为 IDLE，仅 WORKING/WAITING 视为活跃。
 * 应用启动时清空所有状态，避免重启后残留过期数据。
 */
@Slf4j
@Service
public class AgentStatusService {

    private final AgentStatusRepository agentStatusRepository;

    /** DONE 状态过期时间（秒） */
    private static final int DONE_EXPIRE_SECONDS = 30;

    /**
     * 构造函数。启动时清空所有 Agent 状态，避免残留过期数据。
     *
     * @param agentStatusRepository Agent 状态仓库
     */
    public AgentStatusService(AgentStatusRepository agentStatusRepository) {
        this.agentStatusRepository = agentStatusRepository;
        // 启动时清空所有状态，重启后由扫描器重新判断
        agentStatusRepository.deleteAll();
        log.info("已清空所有 Agent 状态（启动初始化）");
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
     * 查询所有活跃的 Agent 状态（仅 WORKING 和 WAITING）。
     * DONE 状态超过 30 秒自动过期为 IDLE，不再视为活跃。
     *
     * @return 活跃 Agent 状态列表
     */
    public List<AgentStatus> getActiveStatuses() {
        LocalDateTime expireThreshold = LocalDateTime.now().minusSeconds(DONE_EXPIRE_SECONDS);
        List<AgentStatus> all = agentStatusRepository.findAll();
        for (AgentStatus s : all) {
            if ("DONE".equals(s.getStatus()) && s.getLastUpdatedAt() != null
                    && s.getLastUpdatedAt().isBefore(expireThreshold)) {
                s.setStatus("IDLE");
                s.setCurrentTool(null);
                agentStatusRepository.save(s);
            }
        }
        return all.stream()
                .filter(s -> "WORKING".equals(s.getStatus()) || "WAITING".equals(s.getStatus()))
                .toList();
    }
}
