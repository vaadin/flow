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
   * last transaction over the write (XHR / heartbeat / endpoint call) resulted
   * in a network error. Flow is attempting to reconnect.
   */
  RECONNECTING = 'reconnecting',

  /**
   * Application has been permanently disconnected due to browser going offline,
   * or the server not being reached after a number of reconnect attempts
   * (see ReconnectDialogConfiguration.java: RECONNECT_ATTEMPTS_KEY).
   */
  CONNECTION_LOST = 'connection-lost'
}

type ConnectionStateChangeListener = (previous: ConnectionState, current: ConnectionState) => void;

export class ConnectionStateStore {

  private connectionState: ConnectionState;

  private stateChangeListeners: Set<ConnectionStateChangeListener> = new Set();

  constructor(initialState: ConnectionState) {
      this.connectionState = initialState;
  }

  addStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.stateChangeListeners.add(listener);
  }

  removeStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.stateChangeListeners.delete(listener);
  }

  get state(): ConnectionState {
    return this.connectionState;
  }

  set state(newState: ConnectionState) {
    if (newState !== this.connectionState) {
      const prevState = this.connectionState;
      this.connectionState = newState;
      for (const listener of this.stateChangeListeners) {
        listener(prevState, this.connectionState);
      }
    }
  }

  get online(): boolean {
    return this.state === ConnectionState.CONNECTED
      || this.state === ConnectionState.LOADING;
  }

  get offline(): boolean {
    return !this.online;
  }
}

const $wnd = window as any;
$wnd.Vaadin = $wnd.Vaadin || {};
$wnd.Vaadin.Flow = $wnd.Vaadin.Flow || {};
$wnd.Vaadin.Flow.connectionState = new ConnectionStateStore(navigator.onLine ? ConnectionState.CONNECTED : ConnectionState.CONNECTION_LOST);
$wnd.addEventListener('online', () => {
  $wnd.Vaadin.Flow.connectionState.state = ConnectionState.CONNECTED;
});
$wnd.addEventListener('offline', () => {
  $wnd.Vaadin.Flow.connectionState.state = ConnectionState.CONNECTION_LOST;
});
