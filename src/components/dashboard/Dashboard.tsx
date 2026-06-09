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
