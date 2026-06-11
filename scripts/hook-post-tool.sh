#!/bin/bash
# Claude Code Hook: PostToolUse
# 从 stdin 读取 JSON 输入，提取 sessionId 和 toolName，通知后端
INPUT=$(cat)
SESSION_ID=$(echo "$INPUT" | grep -o '"session_id":"[^"]*"' | head -1 | cut -d'"' -f4)
TOOL_NAME=$(echo "$INPUT" | grep -o '"tool_name":"[^"]*"' | head -1 | cut -d'"' -f4)
curl -s -X POST http://localhost:8081/api/hooks/event \
  -H "Content-Type: application/json" \
  -d "{\"hookEvent\":\"PostToolUse\",\"toolName\":\"${TOOL_NAME}\",\"sessionId\":\"${SESSION_ID}\"}" \
  > /dev/null 2>&1
