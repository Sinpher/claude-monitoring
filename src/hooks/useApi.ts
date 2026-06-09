import { useState, useEffect } from "react";

/**
 * 通用 API 请求 hook，自动管理 loading 和 error 状态。
 *
 * @param fetchFn 异步请求函数
 * @param deps   依赖数组
 * @returns { data, loading, error }
 */
export function useApi<T>(
  fetchFn: () => Promise<T>,
  deps: unknown[] = []
): { data: T | null; loading: boolean; error: string | null } {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    fetchFn()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, loading, error };
}
