/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

export enum ConnectionState {
  /**
   * Application is connected to server: last transaction over the wire (XHR /
   * heartbeat / endpoint call) was successful.
   */
  CONNECTED = 'connected',

  /**
   * Application is connected and Flow is loading application state from the
   * server, or Hilla is waiting for an endpoint call to return.
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

export type ConnectionStateChangeListener = (previous: ConnectionState, current: ConnectionState) => void;

export class ConnectionStateStore {
  readonly #stateChangeListeners = new Set<ConnectionStateChangeListener>();

  #connectionState: ConnectionState;
  #loadingCount = 0;

  constructor(initialState: ConnectionState) {
    this.#connectionState = initialState;

    if (navigator.serviceWorker) {
      // Query service worker if the most recent fetch was served from cache
      // Add message listener for handling response
      navigator.serviceWorker.addEventListener('message', this.#serviceWorkerMessageListener);
      // Send JSON-RPC request to Vaadin service worker
      void navigator.serviceWorker.ready.then((registration) => {
        registration.active?.postMessage({
          method: 'Vaadin.ServiceWorker.isConnectionLost',
          id: 'Vaadin.ServiceWorker.isConnectionLost'
        });
      });
    }
  }

  addStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.#stateChangeListeners.add(listener);
  }

  removeStateChangeListener(listener: ConnectionStateChangeListener): void {
    this.#stateChangeListeners.delete(listener);
  }

  loadingStarted(): void {
    this.state = ConnectionState.LOADING;
    this.#loadingCount += 1;
  }

  loadingFinished(): void {
    this.#decreaseLoadingCount(ConnectionState.CONNECTED);
  }

  loadingFailed(): void {
    this.#decreaseLoadingCount(ConnectionState.CONNECTION_LOST);
  }

  #decreaseLoadingCount(finalState: ConnectionState) {
    if (this.#loadingCount > 0) {
      this.#loadingCount -= 1;
      if (this.#loadingCount === 0) {
        this.state = finalState;
      }
    }
  }

  get state(): ConnectionState {
    return this.#connectionState;
  }

  set state(newState: ConnectionState) {
    if (newState !== this.#connectionState) {
      const prevState = this.#connectionState;
      this.#connectionState = newState;
      this.#loadingCount = 0;
      for (const listener of this.#stateChangeListeners) {
        listener(prevState, this.#connectionState);
      }
    }
  }

  get online(): boolean {
    return this.#connectionState === ConnectionState.CONNECTED || this.#connectionState === ConnectionState.LOADING;
  }

  get offline(): boolean {
    return !this.online;
  }

  readonly #serviceWorkerMessageListener = (event: MessageEvent) => {
    // Handle JSON-RPC response from service worker
    if (typeof event.data === 'object' && event.data.id === 'Vaadin.ServiceWorker.isConnectionLost') {
      if (event.data.result === true) {
        this.state = ConnectionState.CONNECTION_LOST;
      }

      // Cleanup: remove event listener upon receiving response
      navigator.serviceWorker.removeEventListener('message', this.#serviceWorkerMessageListener);
    }
  };
}

export function isLocalhost(hostname: string): boolean {
  if (hostname === 'localhost') {
    return true;
  }

  if (hostname === '[::1]') {
    return true;
  }

  return /^127\.\d+\.\d+\.\d+$/u.test(hostname);
}

const $wnd = window as any;
if (!$wnd.Vaadin?.connectionState) {
  let online;
  if (isLocalhost(window.location.hostname)) {
    // We do not know if we are online or not as we cannot trust navigator.onLine which checks availability of a network connection. Better to assume online so localhost apps can work
    online = true;
  } else {
    online = navigator.onLine;
  }

  $wnd.Vaadin ??= {};
  $wnd.Vaadin.connectionState = new ConnectionStateStore(
    online ? ConnectionState.CONNECTED : ConnectionState.CONNECTION_LOST
  );
}
