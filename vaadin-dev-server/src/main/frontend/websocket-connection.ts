import { Connection, ConnectionStatus } from './connection';

const WEBSOCKET_FRAGMENT_SIZE = 16384; // This is PushConstants.WEBSOCKET_FRAGMENT_SIZE
const VAADIN_PUSH_JS = 'VAADIN/static/push/vaadinPush.js'; // This is ApplicationConstants.VAADIN_PUSH_DEBUG_JS

export class WebSocketConnection extends Connection {
  static HEARTBEAT_INTERVAL = 180000;

  socket?: any;
  canSend = false;

  constructor(url: string) {
    super();
    if (!url) {
      return;
    }

    const request = {
      transport: 'websocket',
      url,
      contentType: 'application/json; charset=UTF-8',
      reconnect: true,
      reconnectInterval: 5000,
      timeout: -1,
      maxReconnectOnClose: 10000000,
      trackMessageLength: true,
      enableProtocol: true,
      messageDelimiter: '|'
    };
    const handlers = {
      message: (response: any) => {
        const message = { data: response.responseBody };
        this.handleMessage(message);
      },
      // atmosphere.js reports every failed attempt here and goes on to
      // reconnect, so only a connection that is given up on is an error.
      error: () => {
        this.canSend = false;
      },
      failureToReconnect: (_request: any, response: any) => {
        this.canSend = false;
        this.handleError(response);
      },
      open: () => {
        this.canSend = true;
      },
      close: () => {
        this.canSend = false;
      },
      clientTimeout: () => {
        this.canSend = false;
      },
      reconnect: () => {
        this.canSend = false;
      },
      reopen: () => {
        this.canSend = true;
      }
    };

    const onError = (error: any) => {
      this.canSend = false;
      this.handleError(error);
    };
    loadPush().then((atmosphere) => {
      new atmosphere.Atmosphere().subscribe(request, handlers).then((subscription: any) => {
        this.socket = subscription;
      }, onError);
    }, onError);
  }

  onReload(_strategy: string) {
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
        const strategy = json.strategy || 'reload';
        this.onReload(strategy);
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
    if (!this.socket || !this.canSend) {
      waitFor(
        () => this.socket && this.canSend,
        (_atmosphere) => this.send(command, data)
      );
      return;
    }

    const message = JSON.stringify({ command, data });
    const payload = message.length + '|' + message;

    let remaining = payload;
    while (remaining.length) {
      this.socket.push(remaining.substring(0, WEBSOCKET_FRAGMENT_SIZE));
      remaining = remaining.substring(WEBSOCKET_FRAGMENT_SIZE);
    }
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

function loadPush(): Promise<any> {
  // vaadinPush.js is the atmosphere.js ES module. The path is relative to the
  // servlet root, which the document base url points to.
  return import(/* @vite-ignore */ new URL(VAADIN_PUSH_JS, document.baseURI).href);
}
