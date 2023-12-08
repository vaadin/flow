import { Connection, ConnectionStatus } from './connection';

export class WebSocketConnection extends Connection {
  static HEARTBEAT_INTERVAL = 180000;

  socket?: any;

  constructor(url: string) {
    super();
    if (!url) {
        return;
    }

    const config = {
      transport: 'websocket',
      url,
      //fallbackTransport: 'long-polling',
      contentType: 'application/json; charset=UTF-8',
      reconnectInterval: 5000,
      timeout: -1,
      maxReconnectOnClose: 10000000,
      trackMessageLength: true,
      enableProtocol: true,
      handleOnlineOffline: false,
      executeCallbackBeforeReconnect: true,
      messageDelimiter: '|',
      onMessage: (response: any) => {
        const message = { data: response.responseBody };
        this.handleMessage(message);
      },
      onError: (response: any) => {
        this.handleError(response);
      }
    };

    waitForPush().then((atmosphere) => {
      this.socket = atmosphere.subscribe(config);
    });
  }

  onReload() {
    // Intentionally empty
  }

  onUpdate(_path: string, _content: string) {
    // Intentionally empty
  }

  onMessage(_message: any) {}

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
    this.onConnectionError(msg);
  }

  public send(command: string, data: any) {
    if (!this.socket) {
      waitFor(
        () => this.socket,
        (_atmosphere) => this.send(command, data)
      );
      return;
    }

    const message = JSON.stringify({ command, data });
    this.socket.push(message);
  }
}

function waitFor<T>(check: () => T | undefined, callback: (value: T) => void) {
  const value = check();
  if (value) {
    callback(value);
  } else {
    setTimeout(() => waitFor(check, callback), 50);
  }
}

function waitForPush(): Promise<any> {
  // Wait for window.vaadinPush.atmosphere to be defined and return it
  return new Promise<any>((resolve, _reject) => {
    waitFor(() => (window as any)?.vaadinPush?.atmosphere, resolve);
  });
}
