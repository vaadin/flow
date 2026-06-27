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

// TypeScript port of the com.vaadin.client.communication.PushConnection
// interface and its PushConnectionFactory, built alongside the Java version.
// A PushConnection delivers messages to the server over a bidirectional (or
// one-way) push transport; AtmospherePushConnection implements it. The factory
// (GWT.create in Java) produces one for the registry. These are the canonical
// contracts; MessageSender and ConnectionStateHandler reference them.

/** A push connection to the server; mirrors PushConnection.java. */
export interface PushConnection {
  /** Pushes the given payload to the server. */
  push(payload: Record<string, unknown>): void;

  /** Whether the connection is active (connected, or connecting). */
  isActive(): boolean;

  /** Closes the connection, running the command once disconnected. */
  disconnect(command: () => void): void;

  /** The transport type in use (e.g. WEBSOCKET, LONG_POLLING). */
  getTransportType(): string;

  /** Whether the transport is bidirectional (client→server over the same channel). */
  isBidirectional(): boolean;
}

/** Creates a PushConnection for the given registry; mirrors PushConnectionFactory. */
export type PushConnectionFactory = (registry: unknown) => PushConnection;
