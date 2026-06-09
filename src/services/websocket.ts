import { API_BASE, AgentStatus } from "../types";

type StatusCallback = (statuses: AgentStatus[]) => void;

/** WebSocket 客户端，连接后端实时状态推送端点 */
export class AgentStatusWebSocket {
  private ws: WebSocket | null = null;
  private callback: StatusCallback | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  /**
   * 注册状态变更回调。
   *
   * @param callback 回调函数，接收 Agent 状态列表
   */
  onStatusUpdate(callback: StatusCallback): void {
    this.callback = callback;
  }

  /** 连接 WebSocket 端点。 */
  connect(): void {
    const wsUrl = API_BASE.replace(/^http/, "ws") + "/ws/agent-status";
    this.ws = new WebSocket(wsUrl);

    this.ws.onmessage = (event) => {
      try {
        const statuses: AgentStatus[] = JSON.parse(event.data);
        this.callback?.(statuses);
      } catch {
        // 忽略解析错误
      }
    };

    this.ws.onclose = () => {
      // 3 秒后自动重连
      this.reconnectTimer = setTimeout(() => this.connect(), 3000);
    };

    this.ws.onerror = () => {
      this.ws?.close();
    };
  }

  /** 断开 WebSocket 连接。 */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    this.ws?.close();
    this.ws = null;
  }
}
