import { API_BASE, SessionDetail, DailyUsage } from "../types";

/**
 * 获取会话列表。
 *
 * @param start 开始日期（可选）
 * @param end   结束日期（可选）
 * @returns 会话详情列表
 */
export async function fetchSessions(
  start?: string,
  end?: string
): Promise<SessionDetail[]> {
  const params = new URLSearchParams();
  if (start) params.set("start", start);
  if (end) params.set("end", end);
  const query = params.toString() ? `?${params.toString()}` : "";
  const res = await fetch(`${API_BASE}/api/sessions${query}`);
  return res.json();
}

/**
 * 获取单个会话详情。
 *
 * @param id 会话 ID
 * @returns 会话详情
 */
export async function fetchSession(id: string): Promise<SessionDetail> {
  const res = await fetch(`${API_BASE}/api/sessions/${id}`);
  return res.json();
}

/**
 * 获取日用量统计。
 *
 * @param start 开始日期
 * @param end   结束日期
 * @returns 日用量列表
 */
export async function fetchDailyUsage(
  start: string,
  end: string
): Promise<DailyUsage[]> {
  const res = await fetch(
    `${API_BASE}/api/usage/daily?start=${start}&end=${end}`
  );
  return res.json();
}

/**
 * 获取今日用量概览。
 *
 * @returns 今日用量
 */
export async function fetchTodayUsage(): Promise<DailyUsage> {
  const res = await fetch(`${API_BASE}/api/usage/today`);
  return res.json();
}
