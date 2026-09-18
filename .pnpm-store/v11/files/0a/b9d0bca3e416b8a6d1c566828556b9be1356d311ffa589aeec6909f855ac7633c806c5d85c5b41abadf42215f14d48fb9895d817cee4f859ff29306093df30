export declare enum ConnectionState {
    /**
     * Application is connected to server: last transaction over the wire (XHR /
     * heartbeat / endpoint call) was successful.
     */
    CONNECTED = "connected",
    /**
     * Application is connected and Flow is loading application state from the
     * server, or Fusion is waiting for an endpoint call to return.
     */
    LOADING = "loading",
    /**
     * Application has been temporarily disconnected from the server because the
     * last transaction over the wire (XHR / heartbeat / endpoint call) resulted
     * in a network error, or the browser has received the 'online' event and needs
     * to verify reconnection with the server. Flow is attempting to reconnect
     * a configurable number of times before giving up.
     */
    RECONNECTING = "reconnecting",
    /**
     * Application has been permanently disconnected due to browser receiving the
     * 'offline' event, or the server not being reached after a number of reconnect
     * attempts.
     */
    CONNECTION_LOST = "connection-lost"
}
export type ConnectionStateChangeListener = (previous: ConnectionState, current: ConnectionState) => void;
export declare class ConnectionStateStore {
    private connectionState;
    private readonly stateChangeListeners;
    private loadingCount;
    constructor(initialState: ConnectionState);
    addStateChangeListener(listener: ConnectionStateChangeListener): void;
    removeStateChangeListener(listener: ConnectionStateChangeListener): void;
    loadingStarted(): void;
    loadingFinished(): void;
    loadingFailed(): void;
    private decreaseLoadingCount;
    get state(): ConnectionState;
    set state(newState: ConnectionState);
    get online(): boolean;
    get offline(): boolean;
    private serviceWorkerMessageListener;
}
export declare const isLocalhost: (hostname: string) => boolean;
//# sourceMappingURL=ConnectionState.d.ts.map