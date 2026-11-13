export class ChatRoom {
  constructor(private state: DurableObjectState) {
    this.state = state;
  }

  async fetch(request: Request): Promise<Response> {
    if (request.method === "POST" && new URL(request.url).pathname === "/broadcast") {
      const event = await request.text();
      for (const socket of this.state.getWebSockets()) {
        if (socket.readyState === WebSocket.OPEN) {
          try { socket.send(event); } catch { /* disconnected socket */ }
        }
      }
      return new Response(null, { status: 204 });
    }

    if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
      return new Response("WebSocket upgrade required", { status: 426 });
    }
    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];
    this.state.acceptWebSocket(server);
    return new Response(null, { status: 101, webSocket: client });
  }

  webSocketMessage(_socket: WebSocket, _message: string | ArrayBuffer): void {
    // Clients receive server-originated notifications only. Message content is
    // fetched from the authenticated REST endpoint and remains end-to-end encrypted.
  }

  webSocketClose(_socket: WebSocket): void {}
  webSocketError(_socket: WebSocket): void {}
}
