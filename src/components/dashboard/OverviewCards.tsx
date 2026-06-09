import { useApi } from "../../hooks/useApi";
import { fetchTodayUsage } from "../../services/api";

/**
 * 概览卡片组件，显示今日 Token、今日费用、会话数、工具调用。
 */
export function OverviewCards() {
  const { data: todayUsage } = useApi(fetchTodayUsage, []);

  /** 格式化 token 数量为可读字符串 */
  const fmt = (n: number): string => {
    if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
    if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
    return String(n);
  };

  const cards = [
    { label: "今日 Token", value: todayUsage ? fmt(todayUsage.totalInputTokens + todayUsage.totalOutputTokens) : "—", color: "text-[#90caf9]" },
    { label: "今日费用", value: todayUsage ? `$${todayUsage.estimatedCost.toFixed(2)}` : "—", color: "text-[#a5d6a7]" },
    { label: "会话数", value: todayUsage?.sessionCount ?? "—", color: "text-[#ce93d8]" },
    { label: "工具调用", value: todayUsage?.toolCallCount ?? "—", color: "text-[#ffcc80]" },
  ];

  return (
    <div className="grid grid-cols-4 gap-4">
      {cards.map((card) => (
        <div key={card.label} className="bg-cm-card rounded-lg p-4 text-center">
          <div className="text-sm text-cm-muted">{card.label}</div>
          <div className={`text-2xl font-bold ${card.color}`}>{card.value}</div>
        </div>
      ))}
    </div>
  );
}
