# Claude Monitor Tauri 前端实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Claude Monitor 的 Tauri 2.x 桌面应用前端，包含悬浮窗（折叠/展开 + Lottie 动画）和 Web 仪表盘（用量统计 + 趋势图）。

**Architecture:** Tauri 2.x 桌面应用，Rust 端管理窗口生命周期和 SpringBoot 子进程，前端 React + TypeScript 实现 UI。悬浮窗为独立 Tauri 窗口（always_on_top + transparent + 无边框），仪表盘为主窗口。前端通过 HTTP 调用 SpringBoot 后端 API，通过 WebSocket 接收实时状态推送。

**Tech Stack:** Tauri 2.x, React 18, TypeScript, Vite, Lottie-web, ECharts, TailwindCSS

**Spec:** `docs/superpowers/specs/2026-06-08-claude-monitor-design.md`

---

## 文件结构

```
claude-monitoring/
├── src-tauri/
│   ├── Cargo.toml                           # Rust 依赖
│   ├── tauri.conf.json                      # Tauri 配置（窗口、权限）
│   ├── src/
│   │   ├── main.rs                          # Tauri 入口
│   │   └── lib.rs                           # 命令和子进程管理
│   ├── icons/                               # 应用图标
│   └── capabilities/                        # Tauri 2 权限配置
│       └── default.json
├── src/                                     # React 前端
│   ├── main.tsx                             # React 入口
│   ├── App.tsx                              # 路由入口
│   ├── vite-env.d.ts
│   ├── components/
│   │   ├── float/
│   │   │   ├── FloatWindow.tsx              # 悬浮窗主组件
│   │   │   ├── FloatCollapsed.tsx           # 折叠态（圆形动画图标）
│   │   │   ├── FloatExpanded.tsx            # 展开态（信息卡片）
│   │   │   └── StatusIndicator.tsx          # 状态色边框
│   │   ├── dashboard/
│   │   │   ├── Dashboard.tsx                # 仪表盘主页面
│   │   │   ├── OverviewCards.tsx            # 概览卡片
│   │   │   ├── TokenTrend.tsx              # Token 趋势图
│   │   │   ├── ToolDistribution.tsx        # 工具调用分布
│   │   │   └── SessionList.tsx             # 最近会话列表
│   │   └── avatar/
│   │       └── AvatarPlayer.tsx            # Lottie 动画播放器
│   ├── hooks/
│   │   ├── useAgentStatus.ts               # WebSocket 状态 hook
│   │   └── useApi.ts                       # API 请求 hook
│   ├── services/
│   │   ├── api.ts                          # HTTP API 封装
│   │   └── websocket.ts                    # WebSocket 客户端
│   ├── types/
│   │   └── index.ts                        # TypeScript 类型定义
│   ├── assets/
│   │   └── avatars/
│   │       ├── panda/                      # 熊猫形象包
│   │       │   ├── working.json
│   │       │   ├── done.json
│   │       │   ├── waiting.json
│   │       │   └── idle.json
│   │       ├── cat/                        # 猫咪形象包
│   │       └── robot/                     # 机器人形象包
│   └── styles/
│       └── index.css                       # TailwindCSS 入口
├── index.html                               # Vite HTML 入口
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
└── postcss.config.js
```

---

### Task 1: 初始化 Tauri + React 项目

**Files:**
- Create: `package.json`
- Create: `tsconfig.json`
- Create: `vite.config.ts`
- Create: `index.html`
- Create: `src/main.tsx`
- Create: `src/App.tsx`
- Create: `src/vite-env.d.ts`
- Create: `src/styles/index.css`
- Create: `tailwind.config.js`
- Create: `postcss.config.js`
- Create: `src-tauri/Cargo.toml`
- Create: `src-tauri/tauri.conf.json`
- Create: `src-tauri/src/main.rs`
- Create: `src-tauri/src/lib.rs`
- Create: `src-tauri/capabilities/default.json`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "claude-monitoring",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "tauri": "tauri"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "lottie-web": "^5.12.2",
    "echarts": "^5.5.1",
    "echarts-for-react": "^3.0.2"
  },
  "devDependencies": {
    "@tauri-apps/api": "^2.0.0",
    "@tauri-apps/cli": "^2.0.0",
    "@types/react": "^18.3.3",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.3.0",
    "autoprefixer": "^10.4.19",
    "postcss": "^8.4.38",
    "tailwindcss": "^3.4.4",
    "typescript": "^5.5.0",
    "vite": "^5.3.0"
  }
}
```

- [ ] **Step 2: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 3: 创建 vite.config.ts**

```typescript
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const host = process.env.TAURI_DEV_HOST;

export default defineConfig({
  plugins: [react()],
  clearScreenEmpty: false,
  server: {
    port: 1420,
    strictPort: true,
    host: host || false,
    hmr: host
      ? {
          protocol: "ws",
          host,
          port: 1421,
        }
      : undefined,
    watch: {
      ignored: ["**/src-tauri/**"],
    },
  },
});
```

- [ ] **Step 4: 创建 index.html**

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Claude Monitor</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 5: 创建 TailwindCSS 配置**

tailwind.config.js:
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        "cm-bg": "#0d1117",
        "cm-card": "#161b22",
        "cm-border": "#30363d",
        "cm-text": "#e0e0e0",
        "cm-muted": "#888888",
        "cm-working": "#4CAF50",
        "cm-done": "#2196F3",
        "cm-waiting": "#FF9800",
        "cm-idle": "#9E9E9E",
      },
    },
  },
  plugins: [],
};
```

postcss.config.js:
```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

src/styles/index.css:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

body {
  margin: 0;
  background: transparent;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
}
```

- [ ] **Step 6: 创建 React 入口文件**

src/main.tsx:
```tsx
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./styles/index.css";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

src/App.tsx:
```tsx
import { useEffect, useState } from "react";

/**
 * 应用根组件，根据窗口标签决定渲染悬浮窗还是仪表盘。
 */
function App() {
  const [windowLabel, setWindowLabel] = useState<string>("main");

  useEffect(() => {
    // 从 URL 参数读取窗口标签
    const params = new URLSearchParams(window.location.search);
    setWindowLabel(params.get("label") || "main");
  }, []);

  if (windowLabel === "float") {
    return <div className="w-full h-full">悬浮窗（待实现）</div>;
  }

  return <div className="w-full h-full">仪表盘（待实现）</div>;
}

export default App;
```

src/vite-env.d.ts:
```typescript
/// <reference types="vite/client" />
```

- [ ] **Step 7: 创建 Tauri 配置**

src-tauri/Cargo.toml:
```toml
[package]
name = "claude-monitoring"
version = "0.1.0"
edition = "2021"

[lib]
name = "claude_monitoring_lib"
crate-type = ["staticlib", "cdylib", "rlib"]

[build-dependencies]
tauri-build = { version = "2", features = [] }

[dependencies]
tauri = { version = "2", features = [] }
tauri-plugin-shell = "2"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
```

src-tauri/tauri.conf.json:
```json
{
  "$schema": "https://raw.githubusercontent.com/nickel-org/nickel/main/tooling/cli/schema.json",
  "productName": "Claude Monitor",
  "version": "0.1.0",
  "identifier": "com.sinpher.claude-monitor",
  "build": {
    "beforeDevCommand": "npm run dev",
    "devUrl": "http://localhost:1420",
    "beforeBuildCommand": "npm run build",
    "frontendDist": "../dist"
  },
  "app": {
    "windows": [
      {
        "label": "main",
        "title": "Claude Monitor",
        "width": 1200,
        "height": 800,
        "resizable": true,
        "center": true
      },
      {
        "label": "float",
        "url": "/?label=float",
        "title": "",
        "width": 280,
        "height": 320,
        "x": 1600,
        "y": 100,
        "resizable": false,
        "decorations": false,
        "transparent": true,
        "alwaysOnTop": true,
        "skipTaskbar": true,
        "visible": true
      }
    ],
    "security": {
      "csp": null
    }
  },
  "bundle": {
    "active": true,
    "targets": "all",
    "icon": [
      "icons/32x32.png",
      "icons/128x128.png",
      "icons/128x128@2x.png",
      "icons/icon.icns",
      "icons/icon.ico"
    ]
  }
}
```

src-tauri/capabilities/default.json:
```json
{
  "identifier": "default",
  "description": "Capability for the main window",
  "windows": ["main", "float"],
  "permissions": [
    "core:default",
    "shell:allow-execute",
    "shell:allow-spawn",
    "shell:allow-kill"
  ]
}
```

- [ ] **Step 8: 创建 Tauri Rust 入口**

src-tauri/src/main.rs:
```rust
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    claude_monitoring_lib::run()
}
```

src-tauri/src/lib.rs:
```rust
use tauri::Manager;

/// 启动 Tauri 应用，注册窗口和事件。
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            // 启动时显示悬浮窗
            if let Some(float_window) = app.get_webview_window("float") {
                let _ = float_window.show();
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

- [ ] **Step 9: 安装依赖并验证编译**

Run: `cd E:/sinpher/claude-monitoring && npm install`
Run: `cd E:/sinpher/claude-monitoring/src-tauri && cargo check`
Expected: 依赖安装成功，Rust 编译通过

- [ ] **Step 10: Commit**

```bash
git add package.json tsconfig.json vite.config.ts index.html src/ src-tauri/ tailwind.config.js postcss.config.js
git commit -m "初始化 Tauri + React 项目：悬浮窗和仪表盘双窗口"
```

---

### Task 2: TypeScript 类型定义和 API 服务层

**Files:**
- Create: `src/types/index.ts`
- Create: `src/services/api.ts`
- Create: `src/services/websocket.ts`
- Create: `src/hooks/useApi.ts`
- Create: `src/hooks/useAgentStatus.ts`

- [ ] **Step 1: 创建类型定义 src/types/index.ts**

```typescript
/** Agent 工作状态枚举 */
export type AgentStatusType = "WORKING" | "DONE" | "WAITING" | "IDLE";

/** 会话详情 DTO */
export interface SessionDetail {
  id: string;
  project: string;
  model: string;
  startedAt: string;
  endedAt: string | null;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalCacheTokens: number;
  estimatedCost: number;
  toolCallCount: number;
  status: string;
}

/** 日用量 DTO */
export interface DailyUsage {
  date: string;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalCacheTokens: number;
  estimatedCost: number;
  sessionCount: number;
  toolCallCount: number;
}

/** Agent 实时状态 DTO */
export interface AgentStatus {
  sessionId: string;
  status: AgentStatusType;
  currentTool: string | null;
  lastUpdatedAt: string;
}

/** Hook 事件 DTO */
export interface HookEvent {
  hookEvent: string;
  toolName?: string;
  toolInput?: string;
  sessionId: string;
  timestamp?: string;
}

/** 形象包定义 */
export interface AvatarPack {
  name: string;
  label: string;
  animations: Record<AgentStatusType, string>;
}

/** 状态到颜色的映射 */
export const STATUS_COLORS: Record<AgentStatusType, string> = {
  WORKING: "#4CAF50",
  DONE: "#2196F3",
  WAITING: "#FF9800",
  IDLE: "#9E9E9E",
};

/** 状态到中文的映射 */
export const STATUS_LABELS: Record<AgentStatusType, string> = {
  WORKING: "工作中",
  DONE: "已完成",
  WAITING: "等待确认",
  IDLE: "空闲",
};

/** 后端 API 基础 URL */
export const API_BASE = "http://localhost:8080";
```

- [ ] **Step 2: 创建 API 服务 src/services/api.ts**

```typescript
import { API_BASE, SessionDetail, DailyUsage } from "../types";

/**
 * 获取会话列表。
 *
 * @param start 开始日期（可选）
 * @param end   结束日期（可选）
 * @returns 会话详情列表
 */
export async function fetchSessions(
  start?: string,
  end?: string
): Promise<SessionDetail[]> {
  const params = new URLSearchParams();
  if (start) params.set("start", start);
  if (end) params.set("end", end);
  const query = params.toString() ? `?${params.toString()}` : "";
  const res = await fetch(`${API_BASE}/api/sessions${query}`);
  return res.json();
}

/**
 * 获取单个会话详情。
 *
 * @param id 会话 ID
 * @returns 会话详情
 */
export async function fetchSession(id: string): Promise<SessionDetail> {
  const res = await fetch(`${API_BASE}/api/sessions/${id}`);
  return res.json();
}

/**
 * 获取日用量统计。
 *
 * @param start 开始日期
 * @param end   结束日期
 * @returns 日用量列表
 */
export async function fetchDailyUsage(
  start: string,
  end: string
): Promise<DailyUsage[]> {
  const res = await fetch(
    `${API_BASE}/api/usage/daily?start=${start}&end=${end}`
  );
  return res.json();
}

/**
 * 获取今日用量概览。
 *
 * @returns 今日用量
 */
export async function fetchTodayUsage(): Promise<DailyUsage> {
  const res = await fetch(`${API_BASE}/api/usage/today`);
  return res.json();
}
```

- [ ] **Step 3: 创建 WebSocket 客户端 src/services/websocket.ts**

```typescript
import { API_BASE, AgentStatus } from "../types";

type StatusCallback = (statuses: AgentStatus[]) => void;

/** WebSocket 客户端，连接后端实时状态推送端点 */
export class AgentStatusWebSocket {
  private ws: WebSocket | null = null;
  private callback: StatusCallback | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  /**
   * 注册状态变更回调。
   *
   * @param callback 回调函数，接收 Agent 状态列表
   */
  onStatusUpdate(callback: StatusCallback): void {
    this.callback = callback;
  }

  /** 连接 WebSocket 端点。 */
  connect(): void {
    const wsUrl = API_BASE.replace(/^http/, "ws") + "/ws/agent-status";
    this.ws = new WebSocket(wsUrl);

    this.ws.onmessage = (event) => {
      try {
        const statuses: AgentStatus[] = JSON.parse(event.data);
        this.callback?.(statuses);
      } catch {
        // 忽略解析错误
      }
    };

    this.ws.onclose = () => {
      // 3 秒后自动重连
      this.reconnectTimer = setTimeout(() => this.connect(), 3000);
    };

    this.ws.onerror = () => {
      this.ws?.close();
    };
  }

  /** 断开 WebSocket 连接。 */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    this.ws?.close();
    this.ws = null;
  }
}
```

- [ ] **Step 4: 创建 React hooks**

src/hooks/useApi.ts:
```typescript
import { useState, useEffect } from "react";

/**
 * 通用 API 请求 hook，自动管理 loading 和 error 状态。
 *
 * @param fetchFn 异步请求函数
 * @param deps   依赖数组
 * @returns { data, loading, error }
 */
export function useApi<T>(
  fetchFn: () => Promise<T>,
  deps: unknown[] = []
): { data: T | null; loading: boolean; error: string | null } {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    fetchFn()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, loading, error };
}
```

src/hooks/useAgentStatus.ts:
```typescript
import { useState, useEffect, useRef } from "react";
import { AgentStatus, AgentStatusType } from "../types";
import { AgentStatusWebSocket } from "../services/websocket";

/**
 * Agent 实时状态 hook，通过 WebSocket 接收状态推送。
 *
 * @returns { statuses, currentStatus } 当前活跃 Agent 状态列表和主状态
 */
export function useAgentStatus(): {
  statuses: AgentStatus[];
  currentStatus: AgentStatusType;
} {
  const [statuses, setStatuses] = useState<AgentStatus[]>([]);
  const wsRef = useRef<AgentStatusWebSocket | null>(null);

  useEffect(() => {
    const ws = new AgentStatusWebSocket();
    wsRef.current = ws;

    ws.onStatusUpdate((newStatuses) => {
      setStatuses(newStatuses);
    });

    ws.connect();

    return () => {
      ws.disconnect();
    };
  }, []);

  // 推导主状态：优先 WORKING > WAITING > DONE > IDLE
  const currentStatus: AgentStatusType = statuses.length > 0
    ? statuses.some((s) => s.status === "WORKING")
      ? "WORKING"
      : statuses.some((s) => s.status === "WAITING")
      ? "WAITING"
      : statuses.some((s) => s.status === "DONE")
      ? "DONE"
      : "IDLE"
    : "IDLE";

  return { statuses, currentStatus };
}
```

- [ ] **Step 5: 验证 TypeScript 编译**

Run: `cd E:/sinpher/claude-monitoring && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 6: Commit**

```bash
git add src/types/ src/services/ src/hooks/
git commit -m "添加 TypeScript 类型定义和 API/WebSocket 服务层"
```

---

### Task 3: Lottie 动画播放器和形象系统

**Files:**
- Create: `src/components/avatar/AvatarPlayer.tsx`
- Create: `src/assets/avatars/panda/working.json` (简化占位动画)
- Create: `src/assets/avatars/panda/done.json`
- Create: `src/assets/avatars/panda/waiting.json`
- Create: `src/assets/avatars/panda/idle.json`

- [ ] **Step 1: 创建 AvatarPlayer 组件 src/components/avatar/AvatarPlayer.tsx**

```tsx
import { useEffect, useRef } from "react";
import lottie, { AnimationItem } from "lottie-web";
import { AgentStatusType, AvatarPack } from "../../types";

/** 内置熊猫形象包 */
const PANDA_PACK: AvatarPack = {
  name: "panda",
  label: "熊猫",
  animations: {
    WORKING: "/avatars/panda/working.json",
    DONE: "/avatars/panda/done.json",
    WAITING: "/avatars/panda/waiting.json",
    IDLE: "/avatars/panda/idle.json",
  },
};

interface AvatarPlayerProps {
  /** 当前 Agent 状态 */
  status: AgentStatusType;
  /** 形象包名称，默认 panda */
  packName?: string;
  /** 动画尺寸（像素） */
  size?: number;
}

/**
 * Lottie 动画播放器组件，根据 Agent 状态播放对应动画。
 */
export function AvatarPlayer({
  status,
  packName = "panda",
  size = 64,
}: AvatarPlayerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<AnimationItem | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    // 销毁旧动画
    animationRef.current?.destroy();

    const pack = packName === "panda" ? PANDA_PACK : PANDA_PACK;
    const animationPath = pack.animations[status];

    // 加载并播放新动画
    animationRef.current = lottie.loadAnimation({
      container: containerRef.current,
      renderer: "svg",
      loop: status !== "DONE",
      autoplay: true,
      path: animationPath,
    });

    return () => {
      animationRef.current?.destroy();
    };
  }, [status, packName]);

  return (
    <div
      ref={containerRef}
      style={{ width: size, height: size }}
      className="flex items-center justify-center"
    />
  );
}
```

- [ ] **Step 2: 创建简化占位 Lottie 动画文件**

由于真实的熊猫动画需要设计师制作，这里创建简化占位动画（纯色圆形呼吸效果），后续替换为正式动画。

src/assets/avatars/panda/working.json:
```json
{
  "v": "5.7.4",
  "fr": 30,
  "ip": 0,
  "op": 60,
  "w": 64,
  "h": 64,
  "nm": "panda-working",
  "ddd": 0,
  "assets": [],
  "layers": [{
    "ddd": 0,
    "ind": 1,
    "ty": 4,
    "nm": "circle",
    "sr": 1,
    "ks": {
      "o": {"a": 1, "k": [{"t": 0, "s": [80], "e": [100]}, {"t": 30, "s": [100], "e": [80]}, {"t": 60, "s": [80], "e": [80]}]},
      "r": {"a": 0, "k": 0},
      "p": {"a": 0, "k": [32, 32, 0]},
      "a": {"a": 0, "k": [0, 0, 0]},
      "s": {"a": 1, "k": [{"t": 0, "s": [100, 100, 100], "e": [110, 110, 100]}, {"t": 30, "s": [110, 110, 100], "e": [100, 100, 100]}, {"t": 60, "s": [100, 100, 100], "e": [100, 100, 100]}]}
    },
    "ao": 0,
    "shapes": [{
      "ty": "el",
      "d": 1,
      "s": {"a": 0, "k": [48, 48]},
      "p": {"a": 0, "k": [0, 0]},
      "nm": "ellipse"
    }, {
      "ty": "fl",
      "c": {"a": 0, "k": [0.298, 0.686, 0.314, 1]},
      "o": {"a": 0, "k": 100},
      "r": 1,
      "nm": "fill"
    }],
    "ip": 0,
    "op": 60,
    "st": 0,
    "bm": 0
  }]
}
```

src/assets/avatars/panda/done.json:
```json
{
  "v": "5.7.4", "fr": 30, "ip": 0, "op": 60, "w": 64, "h": 64,
  "nm": "panda-done", "ddd": 0, "assets": [],
  "layers": [{
    "ddd": 0, "ind": 1, "ty": 4, "nm": "circle", "sr": 1,
    "ks": {
      "o": {"a": 0, "k": 100},
      "r": {"a": 0, "k": 0},
      "p": {"a": 0, "k": [32, 32, 0]},
      "a": {"a": 0, "k": [0, 0, 0]},
      "s": {"a": 1, "k": [{"t": 0, "s": [80, 80, 100], "e": [100, 100, 100]}, {"t": 20, "s": [100, 100, 100], "e": [100, 100, 100]}]}
    },
    "ao": 0,
    "shapes": [{
      "ty": "el", "d": 1, "s": {"a": 0, "k": [48, 48]}, "p": {"a": 0, "k": [0, 0]}, "nm": "ellipse"
    }, {
      "ty": "fl", "c": {"a": 0, "k": [0.129, 0.588, 0.953, 1]}, "o": {"a": 0, "k": 100}, "r": 1, "nm": "fill"
    }],
    "ip": 0, "op": 60, "st": 0, "bm": 0
  }]
}
```

src/assets/avatars/panda/waiting.json:
```json
{
  "v": "5.7.4", "fr": 30, "ip": 0, "op": 60, "w": 64, "h": 64,
  "nm": "panda-waiting", "ddd": 0, "assets": [],
  "layers": [{
    "ddd": 0, "ind": 1, "ty": 4, "nm": "circle", "sr": 1,
    "ks": {
      "o": {"a": 1, "k": [{"t": 0, "s": [60], "e": [100]}, {"t": 30, "s": [100], "e": [60]}, {"t": 60, "s": [60], "e": [60]}]},
      "r": {"a": 0, "k": 0},
      "p": {"a": 1, "k": [{"t": 0, "s": [32, 32, 0], "e": [32, 28, 0]}, {"t": 15, "s": [32, 28, 0], "e": [32, 32, 0]}, {"t": 30, "s": [32, 32, 0], "e": [32, 28, 0]}, {"t": 45, "s": [32, 28, 0], "e": [32, 32, 0]}, {"t": 60, "s": [32, 32, 0], "e": [32, 32, 0]}]},
      "a": {"a": 0, "k": [0, 0, 0]},
      "s": {"a": 0, "k": [100, 100, 100]}
    },
    "ao": 0,
    "shapes": [{
      "ty": "el", "d": 1, "s": {"a": 0, "k": [48, 48]}, "p": {"a": 0, "k": [0, 0]}, "nm": "ellipse"
    }, {
      "ty": "fl", "c": {"a": 0, "k": [1, 0.596, 0, 1]}, "o": {"a": 0, "k": 100}, "r": 1, "nm": "fill"
    }],
    "ip": 0, "op": 60, "st": 0, "bm": 0
  }]
}
```

src/assets/avatars/panda/idle.json:
```json
{
  "v": "5.7.4", "fr": 30, "ip": 0, "op": 120, "w": 64, "h": 64,
  "nm": "panda-idle", "ddd": 0, "assets": [],
  "layers": [{
    "ddd": 0, "ind": 1, "ty": 4, "nm": "circle", "sr": 1,
    "ks": {
      "o": {"a": 1, "k": [{"t": 0, "s": [40], "e": [70]}, {"t": 60, "s": [70], "e": [40]}, {"t": 120, "s": [40], "e": [40]}]},
      "r": {"a": 0, "k": 0},
      "p": {"a": 0, "k": [32, 32, 0]},
      "a": {"a": 0, "k": [0, 0, 0]},
      "s": {"a": 0, "k": [100, 100, 100]}
    },
    "ao": 0,
    "shapes": [{
      "ty": "el", "d": 1, "s": {"a": 0, "k": [48, 48]}, "p": {"a": 0, "k": [0, 0]}, "nm": "ellipse"
    }, {
      "ty": "fl", "c": {"a": 0, "k": [0.62, 0.62, 0.62, 1]}, "o": {"a": 0, "k": 100}, "r": 1, "nm": "fill"
    }],
    "ip": 0, "op": 120, "st": 0, "bm": 0
  }]
}
```

- [ ] **Step 3: 验证 TypeScript 编译**

Run: `cd E:/sinpher/claude-monitoring && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 4: Commit**

```bash
git add src/components/avatar/ src/assets/avatars/
git commit -m "添加 Lottie 动画播放器和熊猫形象占位动画"
```

---

### Task 4: 悬浮窗组件

**Files:**
- Create: `src/components/float/FloatWindow.tsx`
- Create: `src/components/float/FloatCollapsed.tsx`
- Create: `src/components/float/FloatExpanded.tsx`
- Create: `src/components/float/StatusIndicator.tsx`
- Modify: `src/App.tsx`

- [ ] **Step 1: 创建 StatusIndicator 组件**

src/components/float/StatusIndicator.tsx:
```tsx
import { AgentStatusType, STATUS_COLORS } from "../../types";

interface StatusIndicatorProps {
  /** 当前状态 */
  status: AgentStatusType;
  /** 尺寸（像素） */
  size?: number;
}

/**
 * 状态色圆形边框指示器，用于折叠态悬浮窗。
 */
export function StatusIndicator({ status, size = 64 }: StatusIndicatorProps) {
  const color = STATUS_COLORS[status];

  return (
    <div
      className="rounded-full flex items-center justify-center cursor-pointer transition-all duration-300"
      style={{
        width: size,
        height: size,
        border: `3px solid ${color}`,
        boxShadow: `0 0 12px ${color}40`,
        background: "rgba(13, 17, 23, 0.9)",
      }}
    >
      <div style={{ width: size - 12, height: size - 12 }} />
    </div>
  );
}
```

- [ ] **Step 2: 创建 FloatCollapsed 组件**

src/components/float/FloatCollapsed.tsx:
```tsx
import { AgentStatusType } from "../../types";
import { AvatarPlayer } from "../avatar/AvatarPlayer";
import { StatusIndicator } from "./StatusIndicator";

interface FloatCollapsedProps {
  /** 当前状态 */
  status: AgentStatusType;
  /** 鼠标悬停回调 */
  onHoverStart: () => void;
  /** 点击回调（打开仪表盘） */
  onClick: () => void;
}

/**
 * 悬浮窗折叠态，显示圆形动画图标和状态色边框。
 */
export function FloatCollapsed({ status, onHoverStart, onClick }: FloatCollapsedProps) {
  return (
    <div
      className="relative"
      onMouseEnter={onHoverStart}
    >
      <StatusIndicator status={status} size={64} />
      <div
        className="absolute inset-0 flex items-center justify-center"
        onClick={onClick}
      >
        <AvatarPlayer status={status} size={40} />
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 创建 FloatExpanded 组件**

src/components/float/FloatExpanded.tsx:
```tsx
import { AgentStatusType, STATUS_COLORS, STATUS_LABELS, AgentStatus as AgentStatusType2 } from "../../types";
import { AvatarPlayer } from "../avatar/AvatarPlayer";
import { useApi } from "../../hooks/useApi";
import { fetchTodayUsage } from "../../services/api";

interface FloatExpandedProps {
  /** 当前状态 */
  status: AgentStatusType;
  /** 当前活跃 Agent 列表 */
  agentStatuses: AgentStatusType2[];
  /** 鼠标离开回调 */
  onHoverEnd: () => void;
}

/**
 * 悬浮窗展开态，显示动画形象和实时数据卡片。
 */
export function FloatExpanded({ status, agentStatuses, onHoverEnd }: FloatExpandedProps) {
  const { data: todayUsage } = useApi(fetchTodayUsage, []);
  const currentAgent = agentStatuses.length > 0 ? agentStatuses[0] : null;

  /** 格式化 token 数为可读字符串 */
  const formatTokens = (n: number): string => {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
  };

  /** 格式化费用 */
  const formatCost = (n: number): string => `$${n.toFixed(2)}`;

  return (
    <div
      className="w-[280px] rounded-2xl overflow-hidden"
      style={{
        background: "rgba(13, 17, 23, 0.95)",
        border: `1px solid #30363d`,
        boxShadow: "0 8px 32px rgba(0,0,0,0.5)",
      }}
      onMouseLeave={onHoverEnd}
    >
      {/* 动画区域 */}
      <div
        className="h-[120px] flex items-center justify-center"
        style={{ background: "linear-gradient(135deg, #0d1117 0%, #161b22 100%)" }}
      >
        <div className="text-center">
          <AvatarPlayer status={status} size={56} />
          <div
            className="text-xs mt-1"
            style={{ color: STATUS_COLORS[status] }}
          >
            {STATUS_LABELS[status]}
            {currentAgent?.currentTool && ` · ${currentAgent.currentTool}`}
          </div>
        </div>
      </div>

      {/* 实时数据 */}
      <div className="p-3 grid grid-cols-2 gap-2">
        <div className="bg-white/5 rounded-lg p-2 text-center">
          <div className="text-[11px] text-cm-muted">本次 Token</div>
          <div className="text-base text-[#90caf9] font-bold">
            {todayUsage ? formatTokens(todayUsage.totalInputTokens + todayUsage.totalOutputTokens) : "—"}
          </div>
        </div>
        <div className="bg-white/5 rounded-lg p-2 text-center">
          <div className="text-[11px] text-cm-muted">今日费用</div>
          <div className="text-base text-[#a5d6a7] font-bold">
            {todayUsage ? formatCost(todayUsage.estimatedCost) : "—"}
          </div>
        </div>
        <div className="bg-white/5 rounded-lg p-2 text-center">
          <div className="text-[11px] text-cm-muted">工具调用</div>
          <div className="text-base text-[#ce93d8] font-bold">
            {todayUsage?.toolCallCount ?? "—"}
          </div>
        </div>
        <div className="bg-white/5 rounded-lg p-2 text-center">
          <div className="text-[11px] text-cm-muted">会话数</div>
          <div className="text-base text-[#ffcc80] font-bold">
            {todayUsage?.sessionCount ?? "—"}
          </div>
        </div>
      </div>

      {/* 底部信息 */}
      <div className="px-3 py-1.5 border-t border-cm-border text-[11px] text-cm-muted flex justify-between">
        <span>{currentAgent?.sessionId?.slice(0, 8) || "—"}</span>
        <span>{currentAgent?.currentTool || "—"}</span>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: 创建 FloatWindow 主组件**

src/components/float/FloatWindow.tsx:
```tsx
import { useState, useCallback } from "react";
import { useAgentStatus } from "../../hooks/useAgentStatus";
import { FloatCollapsed } from "./FloatCollapsed";
import { FloatExpanded } from "./FloatExpanded";

/**
 * 悬浮窗主组件，管理折叠/展开状态切换。
 * 折叠态：64x64 圆形动画图标
 * 展开态：280px 宽信息卡片（鼠标悬停 0.5s 后展开）
 */
export function FloatWindow() {
  const { statuses, currentStatus } = useAgentStatus();
  const [expanded, setExpanded] = useState(false);
  const [hoverTimer, setHoverTimer] = useState<ReturnType<typeof setTimeout> | null>(null);

  /** 鼠标悬停开始，0.5s 后展开 */
  const handleHoverStart = useCallback(() => {
    const timer = setTimeout(() => setExpanded(true), 500);
    setHoverTimer(timer);
  }, []);

  /** 鼠标离开，1s 后折叠 */
  const handleHoverEnd = useCallback(() => {
    if (hoverTimer) clearTimeout(hoverTimer);
    setTimeout(() => setExpanded(false), 1000);
  }, [hoverTimer]);

  /** 点击图标打开仪表盘 */
  const handleClick = useCallback(async () => {
    const { getCurrent } = await import("@tauri-apps/api/webviewWindow");
    const mainWindow = getCurrent();
    // 聚焦主窗口（仪表盘）
    if (mainWindow.label === "float") {
      const { WebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      const main = new WebviewWindow("main");
      await main.setFocus();
    }
  }, []);

  return (
    <div className="flex items-start justify-center" data-tauri-drag-region>
      {expanded ? (
        <FloatExpanded
          status={currentStatus}
          agentStatuses={statuses}
          onHoverEnd={handleHoverEnd}
        />
      ) : (
        <FloatCollapsed
          status={currentStatus}
          onHoverStart={handleHoverStart}
          onClick={handleClick}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 5: 更新 App.tsx 集成悬浮窗**

```tsx
import { useEffect, useState } from "react";
import { FloatWindow } from "./components/float/FloatWindow";

/**
 * 应用根组件，根据窗口标签决定渲染悬浮窗还是仪表盘。
 */
function App() {
  const [windowLabel, setWindowLabel] = useState<string>("main");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setWindowLabel(params.get("label") || "main");
  }, []);

  if (windowLabel === "float") {
    return (
      <div className="w-full h-full flex items-center justify-center" data-tauri-drag-region>
        <FloatWindow />
      </div>
    );
  }

  return (
    <div className="w-full h-full bg-cm-bg text-cm-text">
      <div className="p-8 text-center text-cm-muted">仪表盘（待实现）</div>
    </div>
  );
}

export default App;
```

- [ ] **Step 6: 验证 TypeScript 编译**

Run: `cd E:/sinpher/claude-monitoring && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 7: Commit**

```bash
git add src/components/float/ src/App.tsx
git commit -m "添加悬浮窗组件：折叠/展开态和状态色边框"
```

---

### Task 5: Web 仪表盘组件

**Files:**
- Create: `src/components/dashboard/Dashboard.tsx`
- Create: `src/components/dashboard/OverviewCards.tsx`
- Create: `src/components/dashboard/TokenTrend.tsx`
- Create: `src/components/dashboard/ToolDistribution.tsx`
- Create: `src/components/dashboard/SessionList.tsx`
- Modify: `src/App.tsx`

- [ ] **Step 1: 创建 OverviewCards 组件**

src/components/dashboard/OverviewCards.tsx:
```tsx
import { DailyUsage } from "../../types";
import { useApi } from "../../hooks/useApi";
import { fetchTodayUsage } from "../../services/api";

/**
 * 概览卡片组件，显示今日 Token、今日费用、本月累计、会话数。
 */
export function OverviewCards() {
  const { data: todayUsage } = useApi(fetchTodayUsage, []);

  /** 格式化 token */
  const fmt = (n: number): string => {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
  };

  const cards = [
    { label: "今日 Token", value: todayUsage ? fmt(todayUsage.totalInputTokens + todayUsage.totalOutputTokens) : "—", color: "text-[#90caf9]" },
    { label: "今日费用", value: todayUsage ? `$${todayUsage.estimatedCost.toFixed(2)}` : "—", color: "text-[#a5d6a7]" },
    { label: "会话数", value: todayUsage?.sessionCount ?? "—", color: "text-[#ce93d8]" },
    { label: "工具调用", value: todayUsage?.toolCallCount ?? "—", color: "text-[#ffcc80]" },
  ];

  return (
    <div className="grid grid-cols-4 gap-4">
      {cards.map((card) => (
        <div key={card.label} className="bg-cm-card rounded-lg p-4 text-center">
          <div className="text-sm text-cm-muted">{card.label}</div>
          <div className={`text-2xl font-bold ${card.color}`}>{card.value}</div>
        </div>
      ))}
    </div>
  );
}
```

- [ ] **Step 2: 创建 TokenTrend 组件**

src/components/dashboard/TokenTrend.tsx:
```tsx
import { useEffect, useRef } from "react";
import * as echarts from "echarts/core";
import { BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { useApi } from "../../hooks/useApi";
import { fetchDailyUsage } from "../../services/api";

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

/**
 * Token 用量趋势图（近 7 天柱状图）。
 */
export function TokenTrend() {
  const chartRef = useRef<HTMLDivElement>(null);

  // 查询近 7 天数据
  const end = new Date().toISOString().slice(0, 10);
  const start = new Date(Date.now() - 6 * 86400000).toISOString().slice(0, 10);
  const { data: dailyUsage } = useApi(() => fetchDailyUsage(start, end), [start, end]);

  useEffect(() => {
    if (!chartRef.current || !dailyUsage) return;

    const chart = echarts.init(chartRef.current, "dark");
    chart.setOption({
      tooltip: { trigger: "axis" },
      xAxis: {
        type: "category",
        data: dailyUsage.map((d) => d.date.slice(5)),
      },
      yAxis: { type: "value", name: "Token" },
      series: [
        {
          name: "Input",
          type: "bar",
          stack: "token",
          data: dailyUsage.map((d) => d.totalInputTokens),
          itemStyle: { color: "#42A5F5" },
        },
        {
          name: "Output",
          type: "bar",
          stack: "token",
          data: dailyUsage.map((d) => d.totalOutputTokens),
          itemStyle: { color: "#66BB6A" },
        },
      ],
    });

    return () => chart.dispose();
  }, [dailyUsage]);

  return (
    <div className="bg-cm-card rounded-lg p-4">
      <div className="text-sm text-cm-muted mb-2">Token 用量趋势（近 7 天）</div>
      <div ref={chartRef} className="h-[200px]" />
    </div>
  );
}
```

- [ ] **Step 3: 创建 ToolDistribution 组件**

src/components/dashboard/ToolDistribution.tsx:
```tsx
import { fetchSessions } from "../../services/api";
import { useApi } from "../../hooks/useApi";

/**
 * 工具调用分布横向条形图。
 * 从会话列表中提取 toolCallCount 做简化展示。
 */
export function ToolDistribution() {
  const { data: sessions } = useApi(() => fetchSessions(), []);

  // 按项目聚合 toolCallCount
  const projectCounts = sessions
    ? sessions.reduce<Record<string, number>>((acc, s) => {
        acc[s.project] = (acc[s.project] || 0) + s.toolCallCount;
        return acc;
      }, {})
    : {};

  const sorted = Object.entries(projectCounts)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5);

  const maxCount = sorted.length > 0 ? sorted[0][1] : 1;

  return (
    <div className="bg-cm-card rounded-lg p-4">
      <div className="text-sm text-cm-muted mb-3">项目工具调用分布</div>
      <div className="space-y-2">
        {sorted.map(([project, count]) => (
          <div key={project} className="flex items-center gap-2">
            <div
              className="h-2 rounded bg-[#42A5F5]"
              style={{ width: `${(count / maxCount) * 100}%`, minWidth: "4px" }}
            />
            <span className="text-xs text-cm-text truncate max-w-[120px]">{project}</span>
            <span className="text-xs text-cm-muted">{count}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 4: 创建 SessionList 组件**

src/components/dashboard/SessionList.tsx:
```tsx
import { fetchSessions } from "../../services/api";
import { useApi } from "../../hooks/useApi";

/**
 * 最近会话列表组件。
 */
export function SessionList() {
  const { data: sessions } = useApi(() => fetchSessions(), []);

  /** 格式化 token */
  const fmt = (n: number): string => {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
  };

  /** 格式化时间 */
  const fmtTime = (iso: string): string => {
    const d = new Date(iso);
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, "0")}`;
  };

  const recentSessions = sessions?.slice(0, 10) ?? [];

  return (
    <div className="bg-cm-card rounded-lg p-4">
      <div className="text-sm text-cm-muted mb-3">最近会话</div>
      <div className="overflow-x-auto">
        <table className="w-full text-xs">
          <thead>
            <tr className="text-cm-muted border-b border-cm-border">
              <th className="text-left py-2">项目</th>
              <th className="text-left py-2">模型</th>
              <th className="text-right py-2">Token</th>
              <th className="text-right py-2">费用</th>
              <th className="text-right py-2">时间</th>
            </tr>
          </thead>
          <tbody>
            {recentSessions.map((s) => (
              <tr key={s.id} className="border-b border-cm-border/30">
                <td className="py-2 text-cm-text truncate max-w-[200px]">{s.project}</td>
                <td className="py-2 text-cm-muted">{s.model || "—"}</td>
                <td className="py-2 text-right text-[#90caf9]">
                  {fmt(s.totalInputTokens + s.totalOutputTokens)}
                </td>
                <td className="py-2 text-right text-[#a5d6a7]">
                  ${s.estimatedCost.toFixed(2)}
                </td>
                <td className="py-2 text-right text-cm-muted">
                  {s.startedAt ? fmtTime(s.startedAt) : "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: 创建 Dashboard 主页面**

src/components/dashboard/Dashboard.tsx:
```tsx
import { OverviewCards } from "./OverviewCards";
import { TokenTrend } from "./TokenTrend";
import { ToolDistribution } from "./ToolDistribution";
import { SessionList } from "./SessionList";

/**
 * 仪表盘主页面，组合概览卡片、趋势图、工具分布和会话列表。
 */
export function Dashboard() {
  return (
    <div className="min-h-screen bg-cm-bg text-cm-text p-6 space-y-6">
      {/* 顶部导航 */}
      <nav className="flex items-center justify-between bg-cm-card rounded-lg px-4 py-2">
        <span className="font-bold text-cm-text">Claude Monitor</span>
        <div className="flex gap-4 text-sm text-cm-muted">
          <span className="text-cm-working">仪表盘</span>
          <span>设置</span>
        </div>
      </nav>

      {/* 概览卡片 */}
      <OverviewCards />

      {/* 图表区域 */}
      <div className="grid grid-cols-5 gap-4">
        <div className="col-span-3">
          <TokenTrend />
        </div>
        <div className="col-span-2">
          <ToolDistribution />
        </div>
      </div>

      {/* 会话列表 */}
      <SessionList />
    </div>
  );
}
```

- [ ] **Step 6: 更新 App.tsx 集成仪表盘**

```tsx
import { useEffect, useState } from "react";
import { FloatWindow } from "./components/float/FloatWindow";
import { Dashboard } from "./components/dashboard/Dashboard";

/**
 * 应用根组件，根据窗口标签决定渲染悬浮窗还是仪表盘。
 */
function App() {
  const [windowLabel, setWindowLabel] = useState<string>("main");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setWindowLabel(params.get("label") || "main");
  }, []);

  if (windowLabel === "float") {
    return (
      <div className="w-full h-full flex items-center justify-center" data-tauri-drag-region>
        <FloatWindow />
      </div>
    );
  }

  return <Dashboard />;
}

export default App;
```

- [ ] **Step 7: 验证 TypeScript 编译**

Run: `cd E:/sinpher/claude-monitoring && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 8: Commit**

```bash
git add src/components/dashboard/ src/App.tsx
git commit -m "添加 Web 仪表盘：概览卡片、Token 趋势、工具分布、会话列表"
```

---

### Task 6: Tauri 窗口拖拽和右键菜单

**Files:**
- Modify: `src/components/float/FloatWindow.tsx` (添加拖拽和右键菜单)
- Modify: `src-tauri/tauri.conf.json` (添加所需权限)

- [ ] **Step 1: 在 FloatWindow 中添加拖拽支持**

Tauri 2.x 中，添加 `data-tauri-drag-region` 属性的元素可拖拽。已在 Task 4 的 FloatWindow 中添加。需确保悬浮窗的折叠态也可拖拽。

更新 FloatCollapsed.tsx，在外层 div 添加 `data-tauri-drag-region`：

```tsx
// FloatCollapsed.tsx 中最外层 div 添加 data-tauri-drag-region
<div
  className="relative"
  onMouseEnter={onHoverStart}
  data-tauri-drag-region
>
```

- [ ] **Step 2: 添加右键菜单**

在 FloatWindow.tsx 中添加右键菜单功能：

```tsx
// 在 FloatWindow 组件中添加右键菜单状态
const [contextMenu, setContextMenu] = useState<{ x: number; y: number } | null>(null);

// 右键事件处理
const handleContextMenu = useCallback((e: React.MouseEvent) => {
  e.preventDefault();
  setContextMenu({ x: e.clientX, y: e.clientY });
}, []);

// 关闭菜单
const closeMenu = useCallback(() => setContextMenu(null), []);
```

右键菜单项：选择形象、打开仪表盘、退出

- [ ] **Step 3: 在 tauri.conf.json 中添加窗口操作权限**

在 capabilities/default.json 的 permissions 中添加：
```json
"core:window:allow-show",
"core:window:allow-hide",
"core:window:allow-close",
"core:window:allow-set-focus",
"core:window:allow-set-always-on-top"
```

- [ ] **Step 4: 验证编译**

Run: `cd E:/sinpher/claude-monitoring && npx tsc --noEmit`
Expected: 无错误

- [ ] **Step 5: Commit**

```bash
git add src/components/float/ src-tauri/
git commit -m "添加悬浮窗拖拽和右键菜单功能"
```

---

### Task 7: Rust 端子进程管理

**Files:**
- Modify: `src-tauri/src/lib.rs` (添加 SpringBoot 子进程启动/停止逻辑)

- [ ] **Step 1: 在 lib.rs 中添加 SpringBoot 子进程管理**

```rust
use tauri::Manager;
use tauri_plugin_shell::ShellExt;

/// 启动 Tauri 应用，注册窗口和 SpringBoot 子进程管理。
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            // 显示悬浮窗
            if let Some(float_window) = app.get_webview_window("float") {
                let _ = float_window.show();
            }

            // 启动 SpringBoot 后端子进程
            let shell = app.shell();
            let backend_jar = std::env::current_dir()
                .unwrap_or_default()
                .join("backend")
                .join("claude-monitoring-0.1.0.jar");

            // 开发模式下后端由用户手动启动
            // 生产模式下启动打包的 jar
            if backend_jar.exists() {
                let _ = shell.command("java")
                    .args(["-jar", backend_jar.to_str().unwrap_or("")])
                    .spawn();
            }

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

- [ ] **Step 2: 验证 Rust 编译**

Run: `cd E:/sinpher/claude-monitoring/src-tauri && cargo check`
Expected: 编译通过

- [ ] **Step 3: Commit**

```bash
git add src-tauri/src/lib.rs
git commit -m "添加 SpringBoot 子进程管理：生产模式自动启动后端"
```

---

### Task 8: 集成测试与启动验证

- [ ] **Step 1: 安装所有 npm 依赖**

Run: `cd E:/sinpher/claude-monitoring && npm install`

- [ ] **Step 2: 验证前端开发服务器启动**

Run: `cd E:/sinpher/claude-monitoring && npm run dev`
Expected: Vite 开发服务器在 http://localhost:1420 启动

- [ ] **Step 3: 验证 Tauri 开发模式启动**

Run: `cd E:/sinpher/claude-monitoring && npm run tauri dev`
Expected: Tauri 应用启动，显示主窗口和悬浮窗

- [ ] **Step 4: 验证悬浮窗功能**

- 悬浮窗显示圆形动画图标
- 鼠标悬停展开信息卡片
- 鼠标移开自动折叠
- WebSocket 连接后端，状态实时更新

- [ ] **Step 5: 验证仪表盘功能**

- 概览卡片显示今日数据
- Token 趋势图显示近 7 天数据
- 工具分布图显示项目维度统计
- 会话列表显示最近会话

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Tauri 前端 P0 功能完成：悬浮窗 + 仪表盘"
```

---

## 自检结果

**1. Spec 覆盖检查：**
- 实时状态悬浮窗（折叠/展开 + 动画）→ Task 3 + Task 4
- 动画形象系统（4 种状态 + Lottie）→ Task 3
- Web 仪表盘（概览 + 趋势 + 分布 + 会话列表）→ Task 5
- WebSocket 实时推送 → Task 2 (websocket.ts + useAgentStatus)
- REST API 调用 → Task 2 (api.ts + useApi)
- Tauri 双窗口（主窗口 + 悬浮窗）→ Task 1
- 进程管理 → Task 7
- 右键菜单 → Task 6
- 拖拽移动 → Task 6

**2. Placeholder 扫描：** 无 TBD/TODO

**3. 类型一致性：** AgentStatusType、DailyUsage、SessionDetail 等类型在 types/index.ts 中统一定义，所有组件引用一致
