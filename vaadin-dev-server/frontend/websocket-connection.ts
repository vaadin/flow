import { Connection, ConnectionStatus } from './connection';

export class WebSocketConnection extends Connection {
  static HEARTBEAT_INTERVAL = 180000;

  webSocket?: WebSocket;

  constructor(url?: string) {
    super();

    if (url) {
      this.webSocket = new WebSocket(url);
      this.webSocket.onmessage = (msg) => this.handleMessage(msg);
      this.webSocket.onerror = (err) => this.handleError(err);
      this.webSocket.onclose = (_) => {
        if (this.status !== ConnectionStatus.ERROR) {
          this.setStatus(ConnectionStatus.UNAVAILABLE);
        }
        this.webSocket = undefined;
      };
    }

    setInterval(() => {
      if (this.webSocket && self.status !== ConnectionStatus.ERROR && this.status !== ConnectionStatus.UNAVAILABLE) {
        this.webSocket.send('');
      }
    }, WebSocketConnection.HEARTBEAT_INTERVAL);
  }

  onReload() {
    // Intentionally empty
  }

  onUpdate(_path: string, _content: string) {
    // Intentionally empty
  }

  onMessage(message: any) {
    // eslint-disable-next-line no-console
    console.error('Unknown message received from the live reload server:', message);
  }

  handleMessage(msg: any) {
    let json;

    if (msg.data === 'X') {
      // Atmosphere heartbeat message, should be ignored
      return;
    }

    try {
      json = JSON.parse(msg.data);
    } catch (e: any) {
      this.handleError(`[${e.name}: ${e.message}`);
      return;
    }
    if (json.command === 'hello') {
      this.setStatus(ConnectionStatus.ACTIVE);
      this.onHandshake();
    } else if (json.command === 'reload') {
      if (this.status === ConnectionStatus.ACTIVE) {
        this.onReload();
      }
    } else if (json.command === 'update') {
      if (this.status === ConnectionStatus.ACTIVE) {
        this.onUpdate(json.path, json.content);
      }
    } else {
      this.onMessage(json);
    }
  }

  handleError(msg: any) {
    // eslint-disable-next-line no-console
    console.error(msg);
    this.setStatus(ConnectionStatus.ERROR);
    if (msg instanceof Event && this.webSocket) {
      this.onConnectionError(`Error in WebSocket connection to ${this.webSocket.url}`);
    } else {
      this.onConnectionError(msg);
    }
  }

  public send(command: string, data: any) {
    const message = JSON.stringify({ command, data });
    if (!this.webSocket) {
      // eslint-disable-next-line no-console
      console.error(`Unable to send message ${command}. No websocket is available`);
    } else if (this.webSocket.readyState !== WebSocket.OPEN) {
      this.webSocket.addEventListener('open', () => this.webSocket!.send(message));
    } else {
      this.webSocket.send(message);
    }
  }
}
