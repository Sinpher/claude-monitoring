package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hook 事件数据传输对象，接收 Claude Code Hooks 推送的事件数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HookEventDto {

    /** Hook 事件类型：UserPromptSubmit / PreToolUse / PostToolUse / Stop */
    private String hookEvent;

    /** 触发的工具名称（仅 PreToolUse / PostToolUse 有值） */
    private String toolName;

    /** 工具输入参数（仅 PreToolUse / PostToolUse 有值） */
    private String toolInput;

    /** 会话 ID */
    private String sessionId;

    /** 事件时间戳 */
    private String timestamp;
}
