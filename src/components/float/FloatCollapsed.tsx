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
      data-tauri-drag-region
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
