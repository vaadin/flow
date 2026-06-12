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

/** Generic JSON object payload shape (matches Java's {@code JsonObject}). */
export type JsonObject = Record<string, unknown>;

/**
 * Represents the client-side endpoint of a bidirectional ("push") communication
 * channel. Migrated from {@code com.vaadin.client.communication.PushConnection}.
 *
 * The default implementation is {@link AtmospherePushConnection}.
 */
export interface PushConnection {
  /**
   * Pushes a message to the server. Throws if the connection is not active.
   *
   * <p>If the push connection is not connected, the implementation must call
   * {@code ConnectionStateHandler.pushNotConnected(payload)} so the message can
   * be retried later.
   *
   * <p>This method must not be called if the push connection is not
   * bidirectional ({@link isBidirectional} returns false).
   */
  push(payload: JsonObject): void;

  /**
   * Checks whether this push connection is in a state where it can push
   * messages to the server. Active until {@link disconnect} is called.
   */
  isActive(): boolean;

  /**
   * Closes the push connection. Calls the {@code command} once messages can
   * safely be sent through some other communication channel.
   */
  disconnect(command: () => void): void;

  /**
   * Returns a human readable string representation of the transport type used
   * to communicate with the server.
   */
  getTransportType(): string | null;

  /**
   * Checks whether this push connection should be used for communication in
   * both directions or if XHR should be used for client-to-server messages.
   */
  isBidirectional(): boolean;
}
