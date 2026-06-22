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

// Atmosphere-wiring helpers migrated from AtmospherePushConnection.java,
// registered on window.Vaadin.Flow.internal.AtmospherePushConnection by
// registerInternals; the Java methods delegate here. The callback-heavy
// doConnect and the AbstractJSO config accessors stay in Java for now. Also
// bundled to ES5 for the HtmlUnit used by GwtTests.

interface Atmosphere {
  subscribe: (config: unknown) => unknown;
  unsubscribeUrl: (url: string) => void;
}

function atmosphere(): Atmosphere | undefined {
  return (window as unknown as { vaadinPush?: { atmosphere?: Atmosphere } }).vaadinPush?.atmosphere;
}

/** Whether the Atmosphere push library is loaded. */
export function isAtmosphereLoaded(): boolean {
  return !!atmosphere();
}

/** Pushes a message over the given Atmosphere socket. */
export function doPush(socket: unknown, message: string): void {
  (socket as { push: (message: string) => void }).push(message);
}

/** Unsubscribes the Atmosphere connection for the given url. */
export function doDisconnect(url: string): void {
  atmosphere()?.unsubscribeUrl(url);
}
