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

// TypeScript port of the com.vaadin.client.communication.DefaultConnectionStateHandler.Type
// enum. It classifies a connection event by transport (heartbeat / push / XHR)
// with a priority used to decide which competing recoverable error takes
// precedence (a higher-priority error supersedes a lower-priority one).

export const ConnectionMessageType = {
  HEARTBEAT: 'HEARTBEAT',
  PUSH: 'PUSH',
  XHR: 'XHR'
} as const;

export type ConnectionMessageType = (typeof ConnectionMessageType)[keyof typeof ConnectionMessageType];

// Priorities matching the Java enum ordinals: HEARTBEAT(0) < PUSH(1) < XHR(2).
const PRIORITY: Record<ConnectionMessageType, number> = {
  HEARTBEAT: 0,
  PUSH: 1,
  XHR: 2
};

/** Whether the type represents a message transport (push or XHR), not a heartbeat. */
export function isMessage(type: ConnectionMessageType): boolean {
  return type === ConnectionMessageType.PUSH || type === ConnectionMessageType.XHR;
}

/** Whether the first type has higher priority than the second. */
export function isHigherPriorityThan(type: ConnectionMessageType, other: ConnectionMessageType): boolean {
  return PRIORITY[type] > PRIORITY[other];
}
