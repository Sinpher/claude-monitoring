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
