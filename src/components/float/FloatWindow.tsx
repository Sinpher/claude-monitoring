import { useState, useCallback, useRef, useEffect } from "react";
import { useAgentStatus } from "../../hooks/useAgentStatus";
import { useApi } from "../../hooks/useApi";
import { fetchTodayUsage } from "../../services/api";
import { getAvailablePacks } from "../avatar/AvatarPlayer";
import { AvatarPlayer } from "../avatar/AvatarPlayer";
import { STATUS_COLORS, STATUS_LABELS } from "../../types";

/** 折叠态窗口尺寸 */
const COLLAPSED_SIZE = 72;
/** 展开态窗口尺寸 */
const EXPANDED_W = 280;
const EXPANDED_H = 320;
/** 右键菜单展开时的窗口尺寸（需要足够空间显示菜单） */
const MENU_W = 280;
const MENU_H = 400;

/**
 * 悬浮窗主组件，管理折叠/展开状态切换。
 * 先调整窗口大小再渲染内容，避免闪烁。
 */
export function FloatWindow() {
  const { statuses, currentStatus } = useAgentStatus();
  const { data: todayUsage } = useApi(fetchTodayUsage, []);
  const [expanded, setExpanded] = useState(false);
  const [hoverTimer, setHoverTimer] = useState<ReturnType<typeof setTimeout> | null>(null);
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number } | null>(null);
  const [avatarPack, setAvatarPack] = useState<string>("panda");
  const menuRef = useRef<HTMLDivElement>(null);

  /** 调整窗口大小，图标在左上角保持不动 */
  const resizeWindow = useCallback(async (isExpanded: boolean) => {
    try {
      const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      const { LogicalSize } = await import("@tauri-apps/api/dpi");
      const win = getCurrentWebviewWindow();

      if (isExpanded) {
        await win.setSize(new LogicalSize(EXPANDED_W, EXPANDED_H));
      } else {
        await win.setSize(new LogicalSize(COLLAPSED_SIZE, COLLAPSED_SIZE));
      }
    } catch {
      // 非 Tauri 环境忽略
    }
  }, []);

  /** 初始化时缩小到折叠态 */
  useEffect(() => {
    resizeWindow(false);
  }, [resizeWindow]);

  /** 开始拖动窗口 */
  const handleDragStart = useCallback(async (e: React.MouseEvent) => {
    if (e.button !== 0) return;
    try {
      const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      await getCurrentWebviewWindow().startDragging();
    } catch {
      // 非 Tauri 环境忽略
    }
  }, []);

  /** 鼠标悬停1s后展开 */
  const handleHoverStart = useCallback(() => {
    const timer = setTimeout(async () => {
      await resizeWindow(true);
      setExpanded(true);
    }, 1000);
    setHoverTimer(timer);
  }, [resizeWindow]);

  /** 鼠标离开0.8s后折叠 */
  const handleHoverEnd = useCallback(() => {
    if (hoverTimer) clearTimeout(hoverTimer);
    setTimeout(async () => {
      setExpanded(false);
      await resizeWindow(false);
    }, 800);
  }, [hoverTimer, resizeWindow]);

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

  /** 右键菜单：临时扩大窗口以显示完整菜单 */
  const handleContextMenu = useCallback(async (e: React.MouseEvent) => {
    e.preventDefault();
    // 打开菜单前先扩大窗口，避免菜单被裁切
    try {
      const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      const { LogicalSize } = await import("@tauri-apps/api/dpi");
      const win = getCurrentWebviewWindow();
      await win.setSize(new LogicalSize(MENU_W, MENU_H));
    } catch {
      // 非 Tauri 环境忽略
    }
    setContextMenu({ x: e.clientX, y: e.clientY });
  }, []);

  /** 点击外部或窗口失焦时关闭菜单并恢复窗口大小 */
  useEffect(() => {
    if (!contextMenu) return;

    const restoreSize = async () => {
      setContextMenu(null);
      try {
        const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
        const { LogicalSize } = await import("@tauri-apps/api/dpi");
        const win = getCurrentWebviewWindow();
        if (expanded) {
          await win.setSize(new LogicalSize(EXPANDED_W, EXPANDED_H));
        } else {
          await win.setSize(new LogicalSize(COLLAPSED_SIZE, COLLAPSED_SIZE));
        }
      } catch {
        // 非 Tauri 环境忽略
      }
    };

    // 文档内点击关闭
    document.addEventListener("click", restoreSize);

    // 窗口失焦（点击桌面其他区域）关闭
    let unlisten: (() => void) | null = null;
    (async () => {
      try {
        const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
        const win = getCurrentWebviewWindow();
        unlisten = await win.onFocusChanged(({ payload: focused }) => {
          if (!focused) restoreSize();
        });
      } catch {
        // 非 Tauri 环境忽略
      }
    })();

    return () => {
      document.removeEventListener("click", restoreSize);
      if (unlisten) unlisten();
    };
  }, [contextMenu, expanded]);

  /** 关闭右键菜单并恢复窗口大小 */
  const closeContextMenu = useCallback(async () => {
    setContextMenu(null);
    try {
      const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      const { LogicalSize } = await import("@tauri-apps/api/dpi");
      const win = getCurrentWebviewWindow();
      if (expanded) {
        await win.setSize(new LogicalSize(EXPANDED_W, EXPANDED_H));
      } else {
        await win.setSize(new LogicalSize(COLLAPSED_SIZE, COLLAPSED_SIZE));
      }
    } catch {
      // 非 Tauri 环境忽略
    }
  }, [expanded]);
  const handleQuit = useCallback(async () => {
    try {
      const { getCurrentWebviewWindow } = await import("@tauri-apps/api/webviewWindow");
      const win = getCurrentWebviewWindow();
      await win.close();
    } catch {
      window.close();
    }
  }, []);

  const availablePacks = getAvailablePacks();
  const currentAgent = statuses.length > 0 ? statuses[0] : null;
  const statusColor = STATUS_COLORS[currentStatus];

  /** 格式化 token 数 */
  const fmtTokens = (n: number): string => {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
  };

  const totalTokens = todayUsage ? todayUsage.totalInputTokens + todayUsage.totalOutputTokens : 0;

  return (
    <div
      className="relative select-none"
      style={{ background: "transparent" }}
      onMouseDown={handleDragStart}
      onContextMenu={handleContextMenu}
      onMouseLeave={handleHoverEnd}
    >
      {expanded ? (
        <div
          className="w-[280px] rounded-2xl overflow-hidden"
          style={{
            background: "rgba(13, 17, 23, 0.95)",
            border: "1px solid #30363d",
            boxShadow: "0 8px 32px rgba(0,0,0,0.5)",
          }}
        >
          {/* 顶部：图标在左上角，状态文字在右 */}
          <div
            className="flex items-center"
            style={{
              height: COLLAPSED_SIZE,
              padding: 0,
              background: "linear-gradient(135deg, #0d1117 0%, #161b22 100%)",
            }}
          >
            <AvatarPlayer status={currentStatus} size={COLLAPSED_SIZE} packName={avatarPack} />
            <div className="ml-2 text-xs" style={{ color: statusColor }}>
              <div className="font-bold">{STATUS_LABELS[currentStatus]}</div>
              {currentAgent?.currentTool && <div className="text-cm-muted mt-0.5">{currentAgent.currentTool}</div>}
            </div>
          </div>

          {/* 数据卡片 */}
          <div className="p-3 grid grid-cols-2 gap-2">
            <DataCard label="本次 Token" value={todayUsage ? fmtTokens(totalTokens) : "—"} color="#90caf9" />
            <DataCard label="今日费用" value={todayUsage ? `$${todayUsage.estimatedCost.toFixed(2)}` : "—"} color="#a5d6a7" />
            <DataCard label="工具调用" value={todayUsage ? String(todayUsage.toolCallCount) : "—"} color="#ce93d8" />
            <DataCard label="会话数" value={todayUsage ? String(todayUsage.sessionCount) : "—"} color="#ffcc80" />
          </div>

          {/* 底部 */}
          <div className="px-3 py-1.5 border-t border-cm-border text-[11px] text-cm-muted flex justify-between">
            <span>{currentAgent?.sessionId?.slice(0, 8) || "—"}</span>
            <span>{currentAgent?.currentTool || "—"}</span>
          </div>
        </div>
      ) : (
        <div
          className="cursor-pointer"
          onMouseEnter={() => { handleHoverStart(); }}
          onClick={handleClick}
        >
          <AvatarPlayer status={currentStatus} size={COLLAPSED_SIZE} packName={avatarPack} />
        </div>
      )}

      {/* 右键菜单 */}
      {contextMenu && (
        <div
          ref={menuRef}
          className="fixed z-50 bg-cm-card border border-cm-border rounded-lg shadow-xl py-1 text-sm min-w-[140px]"
          style={{ left: contextMenu.x, top: contextMenu.y }}
          onMouseDown={(e) => e.stopPropagation()}
        >
          <div className="px-3 py-1.5 text-cm-muted text-xs">切换形象</div>
          {availablePacks.map((pack) => (
            <button
              key={pack.name}
              className="w-full text-left px-3 py-1.5 hover:bg-white/10 text-cm-text flex items-center gap-2"
              onClick={() => { setAvatarPack(pack.name); closeContextMenu(); }}
            >
              <span
                className="w-3 h-3 rounded-full"
                style={{ background: pack.name === avatarPack ? "#4CAF50" : "transparent", border: pack.name === avatarPack ? "none" : "1px solid #666" }}
              />
              {pack.label}
            </button>
          ))}
          <div className="border-t border-cm-border my-1" />
          <button
            className="w-full text-left px-3 py-1.5 hover:bg-white/10 text-cm-text"
            onClick={() => { closeContextMenu(); handleClick(); }}
          >
            打开仪表盘
          </button>
          <div className="border-t border-cm-border my-1" />
          <button
            className="w-full text-left px-3 py-1.5 hover:bg-white/10 text-red-400"
            onClick={() => { closeContextMenu(); handleQuit(); }}
          >
            退出
          </button>
        </div>
      )}
    </div>
  );
}

/** 数据卡片 */
function DataCard({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <div className="bg-white/5 rounded-lg p-2 text-center">
      <div className="text-[11px] text-cm-muted">{label}</div>
      <div className="text-base font-bold" style={{ color }}>{value}</div>
    </div>
  );
}
