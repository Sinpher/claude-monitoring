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
