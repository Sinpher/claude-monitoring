import { AgentStatusType, STATUS_COLORS, STATUS_LABELS, AgentStatus } from "../../types";
import { AvatarPlayer } from "../avatar/AvatarPlayer";
import { useApi } from "../../hooks/useApi";
import { fetchTodayUsage } from "../../services/api";

interface FloatExpandedProps {
  /** 当前状态 */
  status: AgentStatusType;
  /** 当前活跃 Agent 列表 */
  agentStatuses: AgentStatus[];
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
