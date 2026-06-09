#!/bin/bash
# Claude Monitor Hook 脚本（Linux/Mac）
# 从 stdin 读取 Hook 事件数据，POST 到 SpringBoot 后端

input=$(cat)
curl -s -X POST http://localhost:8080/api/hooks/event \
  -H "Content-Type: application/json" \
  -d "$input" > /dev/null 2>&1
