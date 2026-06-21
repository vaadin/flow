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

// Implementations migrated from ConnectionIndicator.java, registered on
// window.Vaadin.Flow.internal.ConnectionIndicator by registerInternals; the Java
// methods delegate here. These poke window.Vaadin.connectionState /
// connectionIndicator, which are provided by the connection-state component.
// Also bundled to ES5 for the HtmlUnit used by GwtTests.

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

/** Sets the connection state displayed by the loading indicator. */
export function setState(state: string): void {
  const connectionState = vaadin().connectionState;
  if (connectionState) {
    connectionState.state = state;
  }
}

/** Gets the connection state, or null if none is available. */
export function getState(): string | null {
  const connectionState = vaadin().connectionState;
  return connectionState ? connectionState.state : null;
}

/** Sets a property of the connection indicator component. */
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
