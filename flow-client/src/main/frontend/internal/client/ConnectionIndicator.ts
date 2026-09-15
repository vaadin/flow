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

// Implementations migrated from ConnectionIndicator.java. These poke
// window.Vaadin.connectionState / connectionIndicator, which are provided by the
// connection-state component.

/**
 * Application is connected to server: last transaction over the wire (XHR /
 * heartbeat / endpoint call) was successful.
 */
export const CONNECTED = 'connected';

/**
 * Application is connected and Flow is loading application state from the
 * server, or Fusion is waiting for an endpoint call to return.
 */
export const LOADING = 'loading';

/**
 * Application has been temporarily disconnected from the server because the last
 * transaction over the wire (XHR / heartbeat / endpoint call) resulted in a
 * network error, or the browser has received the 'online' event and needs to
 * verify reconnection with the server. Flow is attempting to reconnect a
 * configurable number of times before giving up.
 */
export const RECONNECTING = 'reconnecting';

/**
 * Application has been permanently disconnected due to browser receiving the
 * 'offline' event, or the server not being reached after a number of reconnect
 * attempts.
 */
export const CONNECTION_LOST = 'connection-lost';

interface ConnectionStateApi {
  state: string;
  loadingStarted(): void;
  loadingFinished(): void;
  loadingFailed(): void;
}

function vaadin(): {
  connectionState?: ConnectionStateApi;
  connectionIndicator?: Record<string, unknown>;
} {
  return (
    window as unknown as {
      Vaadin: { connectionState?: ConnectionStateApi; connectionIndicator?: Record<string, unknown> };
    }
  ).Vaadin;
}

/**
 * GWT interface to ConnectionIndicator.ts
 *
 * @param state - the connection state
 */
export function setState(state: string): void {
  const connectionState = vaadin().connectionState;
  if (connectionState) {
    connectionState.state = state;
  }
}

/**
 * Get the connection state.
 *
 * @returns the connection state, or `null` when no connection-state component
 *          is available
 */
export function getState(): string | null {
  const connectionState = vaadin().connectionState;
  return connectionState ? connectionState.state : null;
}

/**
 * Set a property of the connection indicator component.
 *
 * @param property - the property to set
 * @param value - the value to set
 */
export function setProperty(property: string, value: unknown): void {
  const connectionIndicator = vaadin().connectionIndicator;
  if (connectionIndicator) {
    connectionIndicator[property] = value;
  }
}

/** Notifies the connection state that a loading operation has started. */
export function loadingStarted(): void {
  vaadin().connectionState?.loadingStarted();
}

/** Notifies the connection state that a loading operation has finished. */
export function loadingFinished(): void {
  vaadin().connectionState?.loadingFinished();
}

/** Notifies the connection state that a loading operation has failed. */
export function loadingFailed(): void {
  vaadin().connectionState?.loadingFailed();
}
