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

  private stateChangeListeners: Set<ConnectionStateChangeListener> = new Set();

  constructor(initialState: ConnectionState) {
      this.state = initialState;
  }

  getState(): ConnectionState {
    return this.state;
  }

  addStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.stateChangeListeners.add(listener);
  }

  removeStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.stateChangeListeners.delete(listener);
  }

  setState(newState: ConnectionState) {
    if (newState !== this.state) {
      const prevState = this.state;
      this.state = newState;
      for (const listener of this.stateChangeListeners) {
        listener(prevState, this.state);
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
