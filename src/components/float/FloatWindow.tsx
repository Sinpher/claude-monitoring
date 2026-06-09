import { useState, useCallback, useRef, useEffect } from "react";
import { useAgentStatus } from "../../hooks/useAgentStatus";
import { FloatCollapsed } from "./FloatCollapsed";
import { FloatExpanded } from "./FloatExpanded";

/**
 * 悬浮窗主组件，管理折叠/展开状态切换和右键菜单。
 */
export function FloatWindow() {
  const { statuses, currentStatus } = useAgentStatus();
  const [expanded, setExpanded] = useState(false);
  const [hoverTimer, setHoverTimer] = useState<ReturnType<typeof setTimeout> | null>(null);
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number } | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);

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
    try {
      const { WebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      const main = new WebviewWindow("main");
      await main.setFocus();
    } catch {
      // 非 Tauri 环境忽略
    }
  }, []);

  /** 右键菜单 */
  const handleContextMenu = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    setContextMenu({ x: e.clientX, y: e.clientY });
  }, []);

  /** 点击外部关闭菜单 */
  useEffect(() => {
    if (!contextMenu) return;
    const handleClickOutside = () => setContextMenu(null);
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [contextMenu]);

  /** 退出应用 */
  const handleQuit = useCallback(async () => {
    try {
      const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      const win = getCurrentWebviewWindow();
      await win.close();
    } catch {
      window.close();
    }
  }, []);

  return (
    <div
      className="relative"
      onContextMenu={handleContextMenu}
      data-tauri-drag-region
    >
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

      {/* 右键菜单 */}
      {contextMenu && (
        <div
          ref={menuRef}
          className="fixed z-50 bg-cm-card border border-cm-border rounded-lg shadow-xl py-1 text-sm min-w-[140px]"
          style={{ left: contextMenu.x, top: contextMenu.y }}
        >
          <button
            className="w-full text-left px-3 py-1.5 hover:bg-white/10 text-cm-text"
            onClick={() => {
              setContextMenu(null);
            }}
          >
            切换形象
          </button>
          <button
            className="w-full text-left px-3 py-1.5 hover:bg-white/10 text-cm-text"
            onClick={() => {
              setContextMenu(null);
              handleClick();
            }}
          >
            打开仪表盘
          </button>
          <div className="border-t border-cm-border my-1" />
          <button
            className="w-full text-left px-3 py-1.5 hover:bg-white/10 text-red-400"
            onClick={() => {
              setContextMenu(null);
              handleQuit();
            }}
          >
            退出
          </button>
        </div>
      )}
    </div>
  );
}
