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
 * 工具调用记录实体，记录 Claude Code 每次工具调用的详情。
 *
 * @author sinpher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_calls")
public class ToolCall {

    /** 自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属会话 ID */
    @Column(length = 64, nullable = false)
    private String sessionId;

    /** 工具名称（如 Read、Edit、Bash 等） */
    @Column(length = 64, nullable = false)
    private String toolName;

    /** 工具输入参数（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String inputParams;

    /** 调用时间 */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** 耗时（毫秒），可能为 null */
    private Integer duration;
}
