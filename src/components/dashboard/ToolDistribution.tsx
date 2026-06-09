import { fetchSessions } from "../../services/api";
import { useApi } from "../../hooks/useApi";

/**
 * 工具调用分布横向条形图。
 * 从会话列表中提取 toolCallCount 按项目聚合做简化展示。
 */
export function ToolDistribution() {
  const { data: sessions } = useApi(() => fetchSessions(), []);

  // 按项目聚合 toolCallCount
  const projectCounts = sessions
    ? sessions.reduce<Record<string, number>>((acc, s) => {
        acc[s.project] = (acc[s.project] || 0) + s.toolCallCount;
        return acc;
      }, {})
    : {};

  const sorted = Object.entries(projectCounts)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5);

  const maxCount = sorted.length > 0 ? sorted[0][1] : 1;

  return (
    <div className="bg-cm-card rounded-lg p-4">
      <div className="text-sm text-cm-muted mb-3">项目工具调用分布</div>
      <div className="space-y-2">
        {sorted.map(([project, count]) => (
          <div key={project} className="flex items-center gap-2">
            <div
              className="h-2 rounded bg-[#42A5F5]"
              style={{ width: `${(count / maxCount) * 100}%`, minWidth: "4px" }}
            />
            <span className="text-xs text-cm-text truncate max-w-[120px]">{project}</span>
            <span className="text-xs text-cm-muted">{count}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
