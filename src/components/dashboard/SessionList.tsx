import { fetchSessions } from "../../services/api";
import { useApi } from "../../hooks/useApi";

/**
 * 最近会话列表组件。
 */
export function SessionList() {
  const { data: sessions } = useApi(() => fetchSessions(), []);

  /** 格式化 token 数量为可读字符串 */
  const fmt = (n: number): string => {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
  };

  /** 格式化 ISO 时间为简短日期时间 */
  const fmtTime = (iso: string): string => {
    const d = new Date(iso);
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, "0")}`;
  };

  const recentSessions = sessions?.slice(0, 10) ?? [];

  return (
    <div className="bg-cm-card rounded-lg p-4">
      <div className="text-sm text-cm-muted mb-3">最近会话</div>
      <div className="overflow-x-auto">
        <table className="w-full text-xs">
          <thead>
            <tr className="text-cm-muted border-b border-cm-border">
              <th className="text-left py-2">项目</th>
              <th className="text-left py-2">模型</th>
              <th className="text-right py-2">Token</th>
              <th className="text-right py-2">费用</th>
              <th className="text-right py-2">时间</th>
            </tr>
          </thead>
          <tbody>
            {recentSessions.map((s) => (
              <tr key={s.id} className="border-b border-cm-border/30">
                <td className="py-2 text-cm-text truncate max-w-[200px]">{s.project}</td>
                <td className="py-2 text-cm-muted">{s.model || "—"}</td>
                <td className="py-2 text-right text-[#90caf9]">
                  {fmt(s.totalInputTokens + s.totalOutputTokens)}
                </td>
                <td className="py-2 text-right text-[#a5d6a7]">
                  ${s.estimatedCost.toFixed(2)}
                </td>
                <td className="py-2 text-right text-cm-muted">
                  {s.startedAt ? fmtTime(s.startedAt) : "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
