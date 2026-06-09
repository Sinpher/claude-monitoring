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
    const { getCurrentWebviewWindow, WebviewWindow } = await import("@tauri-apps/api/webviewWindow");
    const currentWindow = getCurrentWebviewWindow();
    // 聚焦主窗口（仪表盘）
    if (currentWindow.label === "float") {
      const main = await WebviewWindow.getByLabel("main");
      await main?.setFocus();
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
