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
import { Console } from '../Console';

type UILifecycleLike = { isRunning(): boolean };

/**
 * Queue of server RPC invocations awaiting transmission. Migrated from
 * `com.vaadin.client.communication.ServerRpcQueue`.
 *
 * The flush callback is supplied at construction time (it dispatches into
 * `MessageSender.sendInvocationsToServer`) so the TS class does not need to
 * reach into the Java Registry. A deferred microtask delays the actual flush
 * until after the current event-loop tick — same semantics as the original
 * `Scheduler.get().scheduleDeferred(...)`.
 */
export class ServerRpcQueue {
  private readonly uiLifecycle: UILifecycleLike;
  private readonly send: () => void;
  private pendingInvocations: unknown[] = [];
  private flushPending = false;
  private flushScheduled = false;

  constructor(uiLifecycle: UILifecycleLike, send: () => void) {
    this.uiLifecycle = uiLifecycle;
    this.send = send;
  }

  add(invocation: unknown): void {
    if (!this.uiLifecycle.isRunning()) {
      Console.warn('Trying to invoke method on not yet started or stopped application');
      return;
    }
    this.pendingInvocations.push(invocation);
  }

  clear(): void {
    this.pendingInvocations = [];
    this.flushPending = false;
    this.flushScheduled = false;
  }

  size(): number {
    return this.pendingInvocations.length;
  }

  isEmpty(): boolean {
    return this.size() === 0;
  }

  flush(): void {
    if (this.flushScheduled || this.isEmpty()) {
      return;
    }
    this.flushPending = true;
    this.flushScheduled = true;
    // Deferred so all current-tick event handlers complete before flushing.
    // setTimeout(0) matches the GWT `Scheduler.scheduleDeferred` semantics.
    setTimeout(() => this.doFlush(), 0);
  }

  isFlushPending(): boolean {
    return this.flushPending;
  }

  // eslint-disable-next-line @typescript-eslint/class-methods-use-this
  showLoadingIndicator(): boolean {
    return true;
  }

  toJson(): unknown[] {
    return this.pendingInvocations;
  }

  private doFlush(): void {
    this.flushScheduled = false;
    if (!this.isFlushPending()) {
      // Somebody else cleared the queue before this tick ran.
      return;
    }
    this.send();
  }
}
