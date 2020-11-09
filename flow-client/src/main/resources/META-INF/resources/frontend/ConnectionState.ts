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

  private state: ConnectionState;

  private stateChangeListeners: ConnectionStateChangeListener[] = [];

  constructor(initialState: ConnectionState) {
      this.state = initialState;
  }

  getState(): ConnectionState {
    return this.state;
  }

  addStateChangeListener(listener: ConnectionStateChangeListener): void {
    if (this.stateChangeListeners.indexOf(listener) === -1) {
      this.stateChangeListeners.push(listener);
    }
  }

  removeStateChangeListener(listener: ConnectionStateChangeListener): void {
    const index = this.stateChangeListeners.indexOf(listener);
    this.stateChangeListeners.splice(index);
  }

  setState(newState: ConnectionState) {
    if (newState !== this.state) {
      const prevState = this.state;
      this.state = newState;
      this.stateChangeListeners.forEach(listener => listener(prevState, this.state));
    }
  }

  isOnline(): boolean {
    return this.state === ConnectionState.CONNECTED
      || this.state === ConnectionState.LOADING;
  }

  isOffline(): boolean {
    return !this.isOnline();
  }
}
