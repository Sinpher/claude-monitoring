import { useState, useEffect, useRef } from "react";
import { AgentStatus, AgentStatusType } from "../types";
import { AgentStatusWebSocket } from "../services/websocket";

/**
 * Agent 实时状态 hook，通过 WebSocket 接收状态推送。
 *
 * @returns { statuses, currentStatus } 当前活跃 Agent 状态列表和主状态
 */
export function useAgentStatus(): {
  statuses: AgentStatus[];
  currentStatus: AgentStatusType;
} {
  const [statuses, setStatuses] = useState<AgentStatus[]>([]);
  const wsRef = useRef<AgentStatusWebSocket | null>(null);

  useEffect(() => {
    const ws = new AgentStatusWebSocket();
    wsRef.current = ws;

    ws.onStatusUpdate((newStatuses) => {
      setStatuses(newStatuses);
    });

    ws.connect();

    return () => {
      ws.disconnect();
    };
  }, []);

  // 推导主状态：优先 WORKING > WAITING > DONE > IDLE
  const currentStatus: AgentStatusType = statuses.length > 0
    ? statuses.some((s) => s.status === "WORKING")
      ? "WORKING"
      : statuses.some((s) => s.status === "WAITING")
      ? "WAITING"
      : statuses.some((s) => s.status === "DONE")
      ? "DONE"
      : "IDLE"
    : "IDLE";

  return { statuses, currentStatus };
}
