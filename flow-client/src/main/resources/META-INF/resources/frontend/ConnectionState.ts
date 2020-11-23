export enum ConnectionState {
  /**
   * Application is connected to server: last transaction over the wire (XHR /
   * heartbeat / endpoint call) was successful.
   */
  CONNECTED ='connected',

  /**
   * Application is connected and Flow is loading application state from the
   * server, or Fusion is waiting for an endpoint call to return.
   */
  LOADING = 'loading',

  /**
   * Application has been temporarily disconnected from the server because the
   * last transaction over the wire (XHR / heartbeat / endpoint call) resulted
   * in a network error, or the browser has received the 'online' event and needs
   * to verify reconnection with the server. Flow is attempting to reconnect
   * a configurable number of times before giving up.
   */
  RECONNECTING = 'reconnecting',

  /**
   * Application has been permanently disconnected due to browser receiving the
   * 'offline' event, or the server not being reached after a number of reconnect
   * attempts.
   */
  CONNECTION_LOST = 'connection-lost'
}

type ConnectionStateChangeListener = (previous: ConnectionState, current: ConnectionState) => void;

export class ConnectionStateStore {

  private connectionState: ConnectionState;

  private stateChangeListeners: Set<ConnectionStateChangeListener> = new Set();

  private loadingCount: number = 0;

  constructor(initialState: ConnectionState) {
      this.connectionState = initialState;
  }

  addStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.stateChangeListeners.add(listener);
  }

  removeStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.stateChangeListeners.delete(listener);
  }

  loadingStarted(): void {
    this.state = ConnectionState.LOADING;
    this.loadingCount += 1;
  }

  loadingSucceeded(): void {
    if (this.loadingCount > 0) {
      this.loadingCount -= 1;
      if (this.loadingCount === 0) {
        this.state = ConnectionState.CONNECTED;
      }
    }
  }

  get state(): ConnectionState {
    return this.connectionState;
  }

  set state(newState: ConnectionState) {
    if (newState !== this.connectionState) {
      const prevState = this.connectionState;
      this.connectionState = newState;
      this.loadingCount = 0;
      for (const listener of this.stateChangeListeners) {
        listener(prevState, this.connectionState);
      }
    }
  }

  get online(): boolean {
    return this.connectionState === ConnectionState.CONNECTED
      || this.connectionState === ConnectionState.LOADING;
  }

  get offline(): boolean {
    return !this.online;
  }
}

const $wnd = window as any;
if (!$wnd.Vaadin?.connectionState) {
  $wnd.Vaadin = $wnd.Vaadin || {};
  $wnd.Vaadin.connectionState = new ConnectionStateStore(
    navigator.onLine ? ConnectionState.CONNECTED : ConnectionState.CONNECTION_LOST);
}
