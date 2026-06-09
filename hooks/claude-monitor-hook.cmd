@echo off
REM Claude Monitor Hook 包装脚本（Windows）
REM 从 stdin 读取 Hook 事件数据，POST 到 SpringBoot 后端

setlocal enabledelayedexpansion

REM 读取 stdin 到变量
set "input="
for /f "delims=" %%a in ('more') do set "input=%%a"

REM POST 到后端
curl -s -X POST http://localhost:8080/api/hooks/event -H "Content-Type: application/json" -d "!input!" >nul 2>&1

endlocal
