import { useApi } from "../../hooks/useApi";
import { fetchToolStats } from "../../services/api";

/** 工具颜色映射 */
const TOOL_COLORS: Record<string, string> = {
  Read: "#42A5F5",
  Edit: "#66BB6A",
  Bash: "#FFA726",
  Grep: "#AB47BC",
  Write: "#EF5350",
  Glob: "#26C6DA",
  Agent: "#FFEE58",
};

/**
 * 工具调用分布横向条形图，显示各工具的使用频率和平均耗时。
 */
export function ToolDistribution() {
  const end = new Date().toISOString().slice(0, 10);
  const start = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10);
  const { data: toolStats } = useApi(() => fetchToolStats(start, end), [start, end]);

  const maxCount = toolStats && toolStats.length > 0 ? toolStats[0].count : 1;

  /** 格式化耗时 */
  const fmtDuration = (ms: number): string => {
    if (ms >= 1000) return `${(ms / 1000).toFixed(1)}s`;
    return `${Math.round(ms)}ms`;
  };

  return (
    <div className="bg-cm-card rounded-lg p-4">
      <div className="text-sm text-cm-muted mb-3">工具调用分布（近 30 天）</div>
      <div className="space-y-2">
        {toolStats?.map((tool) => (
          <div key={tool.toolName} className="flex items-center gap-2">
            <div
              className="h-2 rounded"
              style={{
                width: `${(tool.count / maxCount) * 100}%`,
                minWidth: "4px",
                background: TOOL_COLORS[tool.toolName] || "#78909C",
              }}
            />
            <span className="text-xs text-cm-text w-14 truncate">{tool.toolName}</span>
            <span className="text-xs text-cm-muted">{tool.count}</span>
            {tool.avgDuration > 0 && (
              <span className="text-xs text-cm-muted ml-auto">{fmtDuration(tool.avgDuration)}</span>
            )}
          </div>
        ))}
        {(!toolStats || toolStats.length === 0) && (
          <div className="text-xs text-cm-muted">暂无数据</div>
        )}
      </div>
    </div>
  );
}
