import { Connection, ConnectionStatus } from './connection.js';

export class LiveReloadConnection extends Connection {
  webSocket?: WebSocket;

  constructor(url: string) {
    super();
    this.webSocket = new WebSocket(url);
    this.webSocket.onmessage = (msg) => this.handleMessage(msg);
    this.webSocket.onerror = (err) => this.handleError(err);
    this.webSocket.onclose = (_) => {
      if (this.status !== ConnectionStatus.ERROR) {
        this.setStatus(ConnectionStatus.UNAVAILABLE);
      }
      this.webSocket = undefined;
    };

    setInterval(() => {
      if (this.webSocket && self.status !== ConnectionStatus.ERROR && this.status !== ConnectionStatus.UNAVAILABLE) {
        this.webSocket.send('');
      }
    }, Connection.HEARTBEAT_INTERVAL);
  }

  onReload(_strategy: string) {
    // Intentionally empty
  }

  handleMessage(msg: any) {
    let json;

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
        const strategy = json.strategy || 'reload';
        this.onReload(strategy);
      }
    } else {
      this.handleError(`Unknown message from the livereload server: ${msg}`);
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
}
