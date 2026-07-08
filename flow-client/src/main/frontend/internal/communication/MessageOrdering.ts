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

// The server-/sync-id message-ordering kernel of
// com.vaadin.client.communication.MessageHandler, extracted as a standalone,
// pure-logic unit. The server assigns a strictly increasing id to each response;
// the client must process them in order, queueing out-of-order / locked messages
// until the missing ones arrive. MessageHandler composes this.

// com.vaadin.flow.shared.ApplicationConstants
const SERVER_SYNC_ID = 'syncId';
const RESYNCHRONIZE_ID = 'resynchronize';

// The value of an undefined sync id (must be -1 per getLastSeenServerSyncId).
const UNDEFINED_SYNC_ID = -1;

/** A parsed UIDL message (a JSON object). */
export type ValueMap = Record<string, unknown>;

/** The server id of a message, or -1 if it has none. Mirrors getServerId. */
export function getServerId(json: ValueMap): number {
  return SERVER_SYNC_ID in json ? (json[SERVER_SYNC_ID] as number) : -1;
}

/** Whether a message is a resynchronization response. Mirrors isResynchronize. */
export function isResynchronize(json: ValueMap): boolean {
  return RESYNCHRONIZE_ID in json;
}

/**
 * Tracks the last seen server sync id and the queue of pending (out-of-order or
 * locked) UIDL messages, deciding which to handle next. Mirrors the ordering
 * state of MessageHandler.
 */
export class PendingMessageQueue {
  private lastSeenServerSyncId = UNDEFINED_SYNC_ID;

  private pending: ValueMap[] = [];

  /** The last seen server sync id; -1 before any response has been processed. */
  getLastSeenServerSyncId(): number {
    return this.lastSeenServerSyncId;
  }

  setLastSeenServerSyncId(serverId: number): void {
    this.lastSeenServerSyncId = serverId;
  }

  /** The server id the client is currently waiting for. */
  getExpectedServerId(): number {
    return this.lastSeenServerSyncId + 1;
  }

  /** Whether the given server id is the one currently expected (or always-ok). */
  isNextExpectedMessage(serverId: number): boolean {
    if (serverId === -1) {
      return true;
    }
    if (serverId === this.getExpectedServerId()) {
      return true;
    }
    // The first message is always ok.
    return this.lastSeenServerSyncId === UNDEFINED_SYNC_ID;
  }

  /** Whether the given server id has already been seen (a stale re-send). */
  isAlreadySeen(serverId: number): boolean {
    return serverId <= this.lastSeenServerSyncId;
  }

  push(json: ValueMap): void {
    this.pending.push(json);
  }

  isEmpty(): boolean {
    return this.pending.length === 0;
  }

  length(): number {
    return this.pending.length;
  }

  clear(): void {
    this.pending = [];
  }

  /** The index of the next pending message that can be handled now, or -1. */
  findNextHandlable(): number {
    for (let i = 0; i < this.pending.length; i++) {
      if (this.isNextExpectedMessage(getServerId(this.pending[i]))) {
        return i;
      }
    }
    return -1;
  }

  /** Removes and returns the pending message at the given index. */
  remove(index: number): ValueMap {
    return this.pending.splice(index, 1)[0];
  }

  /** Drops pending messages whose server id is older than the expected one. */
  removeOld(): void {
    for (let i = 0; i < this.pending.length; i++) {
      const serverId = getServerId(this.pending[i]);
      if (serverId !== -1 && serverId < this.getExpectedServerId()) {
        this.pending.splice(i, 1);
        i--;
      }
    }
  }
}
