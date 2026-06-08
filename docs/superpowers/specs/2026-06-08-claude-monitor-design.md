# Claude Monitor 设计文档

## 概述

Claude Monitor 是一个桌面监控工具，用于全面监控 Claude Code agent 的工作情况。核心功能包括 Token 用量与成本统计、实时状态悬浮窗（带动画形象）。产品形态为一体化 Tauri 桌面应用，后端使用 SpringBoot 提供数据采集和 API 服务。

## 技术栈

| 层 | 技术 | 说明 |
|---|---|---|
| 后端 | JDK 21 + SpringBoot 3.0 | 数据采集、解析、存储、API |
| 数据库 | SQLite | 嵌入式，零部署 |
| 桌面框架 | Tauri 2.x | Rust 核心 + WebView，轻量（~10MB） |
| 前端 | React + TypeScript | 悬浮窗 + 仪表盘 |
| 动画 | Lottie | 矢量动画，轻量可替换 |
| 图表 | ECharts | 趋势图、分布图 |

## 系统架构

```
┌─────────────────────────────────────────────────┐
│                   数据源                         │
│  ┌──────────────┐    ┌───────────────────────┐  │
│  │  JSONL 文件   │    │  Claude Code Hooks    │  │
│  │  定时扫描 5s  │    │  实时事件推送          │  │
│  └──────┬───────┘    └──────────┬────────────┘  │
└─────────┼───────────────────────┼───────────────┘
          ↓                       ↓
┌─────────────────────────────────────────────────┐
│            SpringBoot 后端                       │
│  ┌──────────┐ ┌──────────┐ ┌────────────────┐  │
│  │ JSONL    │ │ Hooks    │ │  REST API +     │  │
│  │ 解析器   │ │ 接收端   │ │  WebSocket      │  │
│  └────┬─────┘ └────┬─────┘ └───────┬────────┘  │
│       ↓              ↓               │           │
│  ┌──────────────────────────────┐    │           │
│  │         SQLite 数据库        │    │           │
│  └──────────────────────────────┘    │           │
└──────────────────────────────────────┼───────────┘
                                       ↓
┌─────────────────────────────────────────────────┐
│            Tauri 桌面应用                         │
│  ┌────────────────┐  ┌─────────────────────┐   │
│  │   悬浮窗       │  │    Web 仪表盘       │   │
│  │  折叠/展开     │  │  用量/费用/趋势     │   │
│  │  Lottie 动画   │  │  工具分布/会话列表  │   │
│  │  WebSocket     │  │  REST API           │   │
│  └────────────────┘  └─────────────────────┘   │
└─────────────────────────────────────────────────┘
```

## 数据采集

### 方式一：JSONL 文件解析器

**扫描目录**：`C:\Users\{user}\.claude\projects\`

**解析策略**：
- 定时扫描（每 5s）检测新会话文件
- 增量解析：记录每文件的已读取行号，只读新增部分
- 过滤 token=0 的中间消息，只保留最终 assistant 消息的 usage

**提取数据**：
- Token 用量：input_tokens, output_tokens, cache_creation_input_tokens, cache_read_input_tokens
- 工具调用：tool_use 类型的 name + input
- 会话元信息：model, gitBranch, timestamp, version
- 对话内容：user prompt + assistant text（可选存储）

### 方式二：Hooks 实时推送

**Hook 配置**：在 `settings.json` 中注册 Hook 脚本

**Hook 脚本功能**：将事件数据 POST 到 SpringBoot 后端的 `/api/hooks/event`

**捕获的事件**：

| Hook 事件 | 用途 | 状态映射 |
|---|---|---|
| UserPromptSubmit | 用户提交新 prompt | 工作中（绿色边框） |
| PreToolUse | 工具即将执行（含权限请求） | 等待确认（橙色边框，当需要用户批准时） |
| PostToolUse | 工具执行完成 | 执行中（可保持工作中状态） |
| Stop | Agent 停止 | 完成（蓝色边框） |

**Windows 注意**：Hook 脚本需使用 `.cmd` 包装器执行。

## 实时状态悬浮窗

### 交互设计

**折叠态（默认）**：
- 64x64 圆形，显示 Lottie 动画形象
- 状态色边框：绿色=工作中、蓝色=完成、橙色=等待确认、灰色=空闲
- 可拖拽移动位置
- 点击图标 → 打开完整 Web 仪表盘

**展开态（鼠标悬停 0.5s 后）**：
- 平滑动画过渡展开为 280px 宽的信息卡片
- 上方：动画形象 + 状态文字
- 中部：4 格实时数据（本次 Token、本次费用、工具调用数、运行时长）
- 底部：模型名称 + 项目名称
- 鼠标移开 1s 后自动折叠

**右键菜单**：选择形象、打开仪表盘、设置、退出

### 动画形象系统

**4 种状态动画**：
- 工作中：熊猫敲键盘
- 完成：熊猫举 DONE 牌
- 等待确认：熊猫挠头思考
- 空闲：熊猫打盹

**形象替换机制**：
- 每个形象包包含 4 个状态的 Lottie 动画 JSON
- 内置形象：熊猫（默认）、猫咪、机器人
- 用户可放入自定义 Lottie 文件到指定目录
- 右键悬浮窗切换形象

## Web 仪表盘

### 布局

**顶部导航**：仪表盘 / 会话历史 / 设置

**概览卡片（4 格）**：
- 今日 Token
- 今日费用
- 本月累计（含预算进度）
- 今日会话数

**Token 趋势图**：近 7 天用量柱状图

**工具调用分布**：各工具使用频率横向条形图

**最近会话列表**：项目 / 模型 / Token / 费用 / 时长

## 数据模型

### Session（会话）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(PK) | 会话 ID |
| project | VARCHAR | 项目名称 |
| model | VARCHAR | 使用的模型 |
| startedAt | TIMESTAMP | 开始时间 |
| endedAt | TIMESTAMP | 结束时间 |
| totalInputTokens | BIGINT | 输入 token 总量 |
| totalOutputTokens | BIGINT | 输出 token 总量 |
| totalCacheTokens | BIGINT | 缓存 token 总量 |
| estimatedCost | DECIMAL | 预估费用（USD） |
| toolCallCount | INT | 工具调用次数 |
| status | VARCHAR | 进行中/已完成 |

### ToolCall（工具调用）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT(PK) | 自增 ID |
| sessionId | VARCHAR(FK) | 所属会话 |
| toolName | VARCHAR | 工具名称 |
| inputParams | TEXT | 输入参数（JSON） |
| timestamp | TIMESTAMP | 调用时间 |
| duration | INT | 耗时（ms，可选） |

### DailyUsage（日用量）

| 字段 | 类型 | 说明 |
|---|---|---|
| date | DATE(PK) | 日期 |
| totalInputTokens | BIGINT | 输入 token 总量 |
| totalOutputTokens | BIGINT | 输出 token 总量 |
| totalCacheTokens | BIGINT | 缓存 token 总量 |
| estimatedCost | DECIMAL | 预估费用 |
| sessionCount | INT | 会话数 |
| toolCallCount | INT | 工具调用次数 |

### AgentStatus（实时状态）

| 字段 | 类型 | 说明 |
|---|---|---|
| sessionId | VARCHAR(PK) | 会话 ID |
| status | VARCHAR | working/done/waiting/idle |
| currentTool | VARCHAR | 当前执行的工具 |
| lastUpdatedAt | TIMESTAMP | 最后更新时间 |

## 功能优先级

### P0 核心功能（首期实现）
- Token 用量统计（per-session / per-day / per-month）
- 实时状态悬浮窗（折叠/展开 + 动画）
- JSONL 文件增量解析器
- Hooks 事件接收端点

### P1 增强功能（二期）
- Web 仪表盘（概览 + 趋势 + 分布 + 会话列表）
- 形象系统（内置 3 款 + 自定义）
- 工具调用统计（频率、耗时、参数分析）

### P2 未来功能
- 会话历史浏览（对话内容查看、搜索、回放）
- 预算告警（阈值通知）
- 多项目对比分析

## API 设计概要

### 数据查询

- `GET /api/sessions` — 会话列表（分页、筛选）
- `GET /api/sessions/{id}` — 会话详情
- `GET /api/usage/daily` — 日用量统计（日期范围）
- `GET /api/usage/monthly` — 月用量统计
- `GET /api/tools/stats` — 工具调用统计

### 实时通信

- `POST /api/hooks/event` — Hook 事件接收端点
- `WebSocket /ws/agent-status` — Agent 实时状态推送

### 配置

- `GET /api/config/avatar` — 获取当前形象
- `PUT /api/config/avatar` — 切换形象
- `GET /api/config/budget` — 获取预算设置
- `PUT /api/config/budget` — 设置预算

## 成本计算

基于各模型定价，从 token usage 计算费用：

```
费用 = input_tokens × input_price
     + output_tokens × output_price
     + cache_creation_tokens × cache_creation_price
     + cache_read_tokens × cache_read_price
```

模型定价数据从配置文件读取，可随 Anthropic 官方调价更新。

## 进程管理

Tauri 桌面应用启动时，内嵌启动 SpringBoot 后端进程（作为子进程）。应用退出时自动终止后端进程。

- Tauri 主进程：管理窗口生命周期、系统托盘、悬浮窗
- SpringBoot 子进程：数据采集、API 服务，端口从配置读取（默认 8080）
- 前端通过 `http://localhost:8080` 访问后端 API
