package com.sinpher.claudemonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 实时状态实体，记录当前活跃会话的工作状态。
 * 状态值：WORKING / DONE / WAITING / IDLE
 *
 * @author sinpher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_status")
public class AgentStatus {

    /** 会话 ID（主键） */
    @Id
    @Column(length = 64)
    private String sessionId;

    /** 当前状态：WORKING / DONE / WAITING / IDLE */
    @Builder.Default
    @Column(length = 16, nullable = false)
    private String status = "IDLE";

    /** 当前执行的工具名称 */
    @Column(length = 64)
    private String currentTool;

    /** 最后更新时间 */
    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
}
