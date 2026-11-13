export class ChatRoom {
  private sockets = new Set<WebSocket>();

  constructor(private state: DurableObjectState) {
    this.state = state;
  }

  async fetch(request: Request): Promise<Response> {
    const upgrade = request.headers.get("Upgrade");
    if (upgrade?.toLowerCase() !== "websocket") {
      return new Response("WebSocket upgrade required", { status: 426 });
    }
    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];
    this.state.acceptWebSocket(server);
    this.sockets.add(server);
    return new Response(null, { status: 101, webSocket: client });
  }

  webSocketMessage(_socket: WebSocket, message: string | ArrayBuffer): void {
    let parsed: unknown;
    try {
      parsed = typeof message === "string" ? JSON.parse(message) : null;
    } catch {
      return;
    }
    if (!parsed || typeof parsed !== "object") return;
    const outbound = JSON.stringify(parsed);
    for (const socket of this.state.getWebSockets()) {
      if (socket.readyState === WebSocket.OPEN) {
        try { socket.send(outbound); } catch { /* disconnected socket */ }
      }
    }
  }

  webSocketClose(socket: WebSocket): void {
    this.sockets.delete(socket);
  }

  webSocketError(socket: WebSocket): void {
    this.sockets.delete(socket);
  }
}
