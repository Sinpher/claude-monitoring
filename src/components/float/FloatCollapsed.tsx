import { AgentStatusType } from "../../types";
import { AvatarPlayer } from "../avatar/AvatarPlayer";

interface FloatCollapsedProps {
  /** 当前状态 */
  status: AgentStatusType;
  /** 鼠标悬停回调 */
  onHoverStart: () => void;
  /** 点击回调（打开仪表盘） */
  onClick: () => void;
  /** 形象包名称 */
  packName?: string;
}

/**
 * 悬浮窗折叠态，图标充满整个窗口区域。
 */
export function FloatCollapsed({ status, onHoverStart, onClick, packName = "panda" }: FloatCollapsedProps) {
  return (
    <div
      className="cursor-pointer"
      onMouseEnter={onHoverStart}
      onClick={onClick}
    >
      <AvatarPlayer status={status} size={72} packName={packName} />
    </div>
  );
}
