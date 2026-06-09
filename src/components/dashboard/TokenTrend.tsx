import { useEffect, useRef } from "react";
import * as echarts from "echarts/core";
import { BarChart } from "echarts/charts";
import { GridComponent, TooltipComponent } from "echarts/components";
import { CanvasRenderer } from "echarts/renderers";
import { useApi } from "../../hooks/useApi";
import { fetchDailyUsage } from "../../services/api";

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer]);

/**
 * Token 用量趋势图（近 7 天柱状图）。
 */
export function TokenTrend() {
  const chartRef = useRef<HTMLDivElement>(null);

  // 查询近 7 天数据
  const end = new Date().toISOString().slice(0, 10);
  const start = new Date(Date.now() - 6 * 86400000).toISOString().slice(0, 10);
  const { data: dailyUsage } = useApi(() => fetchDailyUsage(start, end), [start, end]);

  useEffect(() => {
    if (!chartRef.current || !dailyUsage) return;

    const chart = echarts.init(chartRef.current, "dark");
    chart.setOption({
      tooltip: { trigger: "axis" },
      xAxis: {
        type: "category",
        data: dailyUsage.map((d) => d.date.slice(5)),
      },
      yAxis: { type: "value", name: "Token" },
      series: [
        {
          name: "Input",
          type: "bar",
          stack: "token",
          data: dailyUsage.map((d) => d.totalInputTokens),
          itemStyle: { color: "#42A5F5" },
        },
        {
          name: "Output",
          type: "bar",
          stack: "token",
          data: dailyUsage.map((d) => d.totalOutputTokens),
          itemStyle: { color: "#66BB6A" },
        },
      ],
    });

    return () => chart.dispose();
  }, [dailyUsage]);

  return (
    <div className="bg-cm-card rounded-lg p-4">
      <div className="text-sm text-cm-muted mb-2">Token 用量趋势（近 7 天）</div>
      <div ref={chartRef} className="h-[200px]" />
    </div>
  );
}
