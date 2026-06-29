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

// Implementations migrated from MessageHandler.java, registered on
// window.Vaadin.Flow.internal.MessageHandler by registerInternals; the Java
// methods delegate here. Also bundled to ES5 for the HtmlUnit used by GwtTests.
//
// The MessageHandler class below is the build-alongside TS port of the rest of
// MessageHandler.java: it handles incoming UIDL messages in server-sync-id order
// (queueing out-of-order / locked messages), then applies each (constants ->
// ConstantPool, changes -> TreeChangeProcessor, executeJs, dependency loading,
// session-expired/error handling). It composes the ported MessageOrdering
// (PendingMessageQueue), TreeChangeProcessor, DomApi, Reactive,
// EagerDependencyTracker, and the JSNI helpers above; everything else is a
// Registry contract satisfied at cutover.

import { getServerId, isResynchronize, PendingMessageQueue } from './communication/MessageOrdering';
import { ResynchronizationState } from './communication/ResynchronizationState';
import { updateApiImplementation } from './dom/DomApi';
import { runWhenEagerDependenciesLoaded } from './EagerDependencyTracker';
import { Reactive } from './reactive/reactive';
import { processChanges as applyTreeChanges } from './TreeChangeProcessor';
import { UIState } from './UILifecycle';

/** Removes the link and style elements with the given dependency id. */
export function removeStylesheetByIdFromDom(dependencyId: string): void {
  const elements = document.querySelectorAll(`link[data-id="${dependencyId}"], style[data-id="${dependencyId}"]`);
  for (const element of Array.from(elements)) {
    element.remove();
  }
}

/** Invokes the element's afterServerUpdate callback if it defines one. */
export function callAfterServerUpdates(node: Node): void {
  const target = node as unknown as { afterServerUpdate?: () => void };
  if (node && target.afterServerUpdate) {
    target.afterServerUpdate();
  }
}

/** Milliseconds from the navigation response start to now, or -1 if unknown. */
export function calculateBootstrapTime(): number {
  const perf = window.performance as (Performance & { timing?: { responseStart: number } }) | undefined;
  if (perf && perf.timing) {
    return Date.now() - perf.timing.responseStart;
  }
  return -1;
}

/** Parses a JSON UIDL response into a ValueMap-compatible object. */
export function parseJSONResponse(jsonText: string): unknown {
  return JSON.parse(jsonText);
}

/** The navigation fetchStart timestamp, or 0 if unknown. */
export function getFetchStartTime(): number {
  const perf = window.performance as (Performance & { timing?: { fetchStart?: number } }) | undefined;
  if (perf && perf.timing && perf.timing.fetchStart) {
    return perf.timing.fetchStart;
  }
  return 0;
}

// com.vaadin.flow.shared.ApplicationConstants / JsonConstants
const CSRF_TOKEN_DEFAULT_VALUE = 'init';
const CLIENT_TO_SERVER_ID = 'clientId';
const UIDL_SECURITY_TOKEN_ID = 'Vaadin-Security-Key';
const UIDL_PUSH_ID = 'Vaadin-Push-ID';
const UIDL_KEY_EXECUTE = 'execute';
const META_SESSION_EXPIRED = 'sessionExpired';
const META_ASYNC = 'async';

const SESSION_EXPIRED_HANDLING_DELAY = 250;

/** A parsed UIDL message. */
type ValueMap = Record<string, unknown>;

/** The slice of Registry MessageHandler uses. */
interface MessageHandlerRegistry {
  getUILifecycle(): { getState(): string; setState(state: string): void };
  getMessageSender(): {
    getResynchronizationState(): string;
    clearResynchronizationState(): void;
    setClientToServerMessageId(nextExpectedId: number, force: boolean): void;
    requestResynchronize(): boolean;
    resynchronize(): void;
  };
  getStateTree(): unknown & { prepareForResync(): void };
  getRequestResponseTracker(): { fireResponseHandlingStarted(): void; endRequest(): void; hasActiveRequest(): boolean };
  getLoadingIndicatorStateHandler(): { stopLoading(): void };
  getConstantPool(): { importFromJson(constants: unknown): void };
  getExecuteJavaScriptProcessor(): { execute(commands: unknown): void };
  getDependencyLoader(): {
    loadDependencies(dependencies: Map<string, unknown[]>): void;
    requireHtmlImportsReady(): void;
  };
  getSystemErrorHandler(): {
    handleSessionExpiredError(details: string | null): void;
    handleUnrecoverableError(
      caption: string,
      message: string,
      details: string,
      url: string,
      querySelector: string | null
    ): void;
  };
  getApplicationConfiguration(): { getMaxMessageSuspendTimeout(): number };
  getResourceLoader(): { clearLoadedResourceById(dependencyId: string): void };
}

/** A state node whose DOM updates should be flushed after a server message. */
interface UpdatedNode {
  isUnregistered(): boolean;
  getDomNode(): Node | null;
}

/** Handles incoming UIDL messages and applies them to the client; mirrors MessageHandler.java. */
export class MessageHandler {
  private readonly registry: MessageHandlerRegistry;

  // Locks; while non-empty, response handling is suspended.
  private readonly responseHandlingLocks = new Set<object>();

  // The server-sync-id ordering state + the queue of pending messages.
  private readonly ordering = new PendingMessageQueue();

  private csrfToken = CSRF_TOKEN_DEFAULT_VALUE;

  private pushId: string | null = null;

  private bootstrapTime = 0;

  // Profiling timings (ms), exposed via getProfilingData / client.getProfilingData.
  private lastProcessingTime = 0;

  private totalProcessingTime = 0;

  private serverTimingInfo: number[] | null = null;

  private initialMessageHandled = false;

  private forceHandleMessage: ReturnType<typeof setTimeout> | null = null;

  private nextResponseSessionExpiredHandler: (() => void) | null = null;

  constructor(registry: MessageHandlerRegistry) {
    this.registry = registry;
  }

  /** Handles a received UIDL message, starting the UI on the first one. */
  handleMessage(json: ValueMap): void {
    if (getServerId(json) === -1) {
      const meta = json.meta as ValueMap | undefined;
      if (!meta || !(META_SESSION_EXPIRED in meta)) {
        console.error(
          "Response didn't contain a server id. " +
            'Please verify that the server is up-to-date and that the response data has not been modified in transmission.'
        );
      }
    }

    let state = this.registry.getUILifecycle().getState();
    if (state === UIState.INITIALIZING) {
      state = UIState.RUNNING;
      this.registry.getUILifecycle().setState(state);
    }
    if (state === UIState.RUNNING) {
      this.handleJSON(json);
    } else {
      console.warn('Ignored received message because application has already been stopped');
    }
  }

  private handleJSON(valueMap: ValueMap): void {
    const serverId = getServerId(valueMap);
    const hasResynchronize = isResynchronize(valueMap);

    if (
      !hasResynchronize &&
      this.registry.getMessageSender().getResynchronizationState() === ResynchronizationState.WAITING_FOR_RESPONSE
    ) {
      if (UIDL_KEY_EXECUTE in valueMap) {
        const commands = valueMap[UIDL_KEY_EXECUTE] as unknown[][];
        for (const command of commands) {
          if (command.length > 0 && command[0] === 'window.location.reload();') {
            console.warn('Executing forced page reload while a resync request is ongoing.');
            window.location.reload();
            return;
          }
        }
      }
      console.warn('Queueing message from the server as a resync request is ongoing.');
      this.ordering.push(valueMap);
      return;
    }

    this.registry.getMessageSender().clearResynchronizationState();

    if (hasResynchronize && !this.ordering.isNextExpectedMessage(serverId)) {
      this.ordering.setLastSeenServerSyncId(serverId - 1);
      this.ordering.removeOld();
    }

    const locked = this.responseHandlingLocks.size > 0;
    if (locked || !this.ordering.isNextExpectedMessage(serverId)) {
      if (!locked) {
        if (this.ordering.isAlreadySeen(serverId)) {
          console.warn(`Received message with server id ${serverId} but have already seen a newer one. Ignoring it`);
          this.endRequestIfResponse(valueMap);
          return;
        }
      }
      this.ordering.push(valueMap);
      if (this.forceHandleMessage === null) {
        const timeout = this.registry.getApplicationConfiguration().getMaxMessageSuspendTimeout();
        this.forceHandleMessage = setTimeout(() => this.forceMessageHandling(), timeout);
      }
      return;
    }

    if (hasResynchronize) {
      // Unregister all nodes and rebuild the state tree.
      this.registry.getStateTree().prepareForResync();
    }

    const lock = {};
    this.suspendResponseHandling(lock);

    this.registry.getRequestResponseTracker().fireResponseHandlingStarted();
    // Client id must be updated before server id (a server-id update can trigger
    // a resync that must use the updated client id).
    if (CLIENT_TO_SERVER_ID in valueMap) {
      this.registry
        .getMessageSender()
        .setClientToServerMessageId(valueMap[CLIENT_TO_SERVER_ID] as number, hasResynchronize);
    }
    if (serverId !== -1) {
      this.ordering.setLastSeenServerSyncId(serverId);
    }

    if ('redirect' in valueMap) {
      const url = (valueMap.redirect as ValueMap).url as string;
      window.location.href = url;
      return;
    }
    if (UIDL_SECURITY_TOKEN_ID in valueMap) {
      this.csrfToken = valueMap[UIDL_SECURITY_TOKEN_ID] as string;
    }
    if (UIDL_PUSH_ID in valueMap) {
      this.pushId = valueMap[UIDL_PUSH_ID] as string;
    }

    this.handleDependencies(valueMap);
    if (!this.initialMessageHandled) {
      this.registry.getDependencyLoader().requireHtmlImportsReady();
    }

    runWhenEagerDependenciesLoaded(() => updateApiImplementation());
    runWhenEagerDependenciesLoaded(() => this.processMessage(valueMap, lock));
  }

  private handleDependencies(inputJson: ValueMap): void {
    const dependencies = new Map<string, unknown[]>();
    for (const loadMode of ['INLINE', 'EAGER', 'LAZY']) {
      if (loadMode in inputJson) {
        dependencies.set(loadMode, inputJson[loadMode] as unknown[]);
      }
    }
    if (dependencies.size > 0) {
      this.registry.getDependencyLoader().loadDependencies(dependencies);
    }
  }

  private processMessage(valueMap: ValueMap, lock: object): void {
    const start = performance.now();
    if ('timings' in valueMap) {
      this.serverTimingInfo = valueMap.timings as number[];
    }
    try {
      if ('constants' in valueMap) {
        this.registry.getConstantPool().importFromJson(valueMap.constants);
      }
      if ('changes' in valueMap) {
        this.processChanges(valueMap);
      }
      if ('stylesheetRemovals' in valueMap) {
        this.processStylesheetRemovals(valueMap.stylesheetRemovals as string[]);
      }
      if (UIDL_KEY_EXECUTE in valueMap) {
        // Invoke JS only after all tree changes and post-flush listeners added
        // during message processing (hence the doubly-nested post-flush).
        Reactive.addPostFlushListener(() =>
          Reactive.addPostFlushListener(() =>
            this.registry.getExecuteJavaScriptProcessor().execute(valueMap[UIDL_KEY_EXECUTE])
          )
        );
      }

      Reactive.flush();

      const meta = valueMap.meta as ValueMap | undefined;
      if (meta) {
        if (META_SESSION_EXPIRED in meta) {
          if (this.nextResponseSessionExpiredHandler !== null) {
            this.nextResponseSessionExpiredHandler();
          } else if (this.registry.getUILifecycle().getState() !== UIState.TERMINATED) {
            this.registry.getUILifecycle().setState(UIState.TERMINATED);
            // Delay so a pending redirect/reload is not cancelled.
            setTimeout(
              () => this.registry.getSystemErrorHandler().handleSessionExpiredError(null),
              SESSION_EXPIRED_HANDLING_DELAY
            );
          }
        } else if ('appError' in meta && this.registry.getUILifecycle().getState() !== UIState.TERMINATED) {
          const error = meta.appError as ValueMap;
          this.registry
            .getSystemErrorHandler()
            .handleUnrecoverableError(
              error.caption as string,
              error.message as string,
              error.details as string,
              error.url as string,
              error.querySelector as string | null
            );
          this.registry.getUILifecycle().setState(UIState.TERMINATED);
        }
      }
      this.nextResponseSessionExpiredHandler = null;
    } finally {
      // Mark the initial UIDL handled and end the request in finally so the UI
      // settles (ApplicationConnection.isActive returns false) even if applying
      // the message threw. In GWT the equivalent work ran inside $entry, so an
      // uncaught error never left the client perpetually "active"; here we
      // guarantee the same by not gating these on successful processing.
      if (!this.initialMessageHandled) {
        this.initialMessageHandled = true;
        this.bootstrapTime = calculateBootstrapTime();
      }
      this.lastProcessingTime = Math.round(performance.now() - start);
      this.totalProcessingTime += this.lastProcessingTime;
      this.endRequestIfResponse(valueMap);
      this.resumeResponseHandling(lock);
    }
  }

  /**
   * Profiling data for the last response: last and total processing time, the
   * optional server timing info, and the bootstrap time. Mirrors the
   * getProfilingData JSNI in ApplicationConnection.java.
   */
  getProfilingData(): number[] {
    const data = [this.lastProcessingTime, this.totalProcessingTime];
    if (this.serverTimingInfo !== null) {
      data.push(...this.serverTimingInfo);
    } else {
      data.push(-1, -1);
    }
    data.push(this.bootstrapTime);
    return data;
  }

  private processChanges(json: ValueMap): void {
    const tree = this.registry.getStateTree();
    // Error/meta responses (e.g. an unrecoverable error) carry "changes":{} — an
    // empty object, not an array. GWT's JsonArray.length() treated that as zero
    // changes; here a non-array would make `for...of` throw and abort before the
    // meta.appError handling runs, so coerce it to an empty list.
    const changes = Array.isArray(json.changes) ? (json.changes as Array<Record<string, unknown>>) : [];
    // The real StateTree satisfies TreeChangeProcessor's contract at cutover.
    const updatedNodes = applyTreeChanges(tree as never, changes);
    Reactive.addPostFlushListener(() =>
      setTimeout(() => updatedNodes.forEach((node) => this.afterServerUpdates(node as unknown as UpdatedNode)), 0)
    );
  }

  private afterServerUpdates(node: UpdatedNode): void {
    if (!node.isUnregistered()) {
      const domNode = node.getDomNode();
      if (domNode) {
        callAfterServerUpdates(domNode);
      }
    }
  }

  private processStylesheetRemovals(removals: string[]): void {
    for (const dependencyId of removals) {
      removeStylesheetByIdFromDom(dependencyId);
      this.registry.getResourceLoader().clearLoadedResourceById(dependencyId);
    }
  }

  private endRequestIfResponse(json: ValueMap): void {
    if (this.isResponse(json)) {
      this.registry.getRequestResponseTracker().endRequest();
      this.registry.getLoadingIndicatorStateHandler().stopLoading();
    }
  }

  private isResponse(json: ValueMap): boolean {
    const meta = json.meta as ValueMap | undefined;
    return !meta || !(META_ASYNC in meta);
  }

  /** Postpones response rendering until the lock is released. */
  suspendResponseHandling(lock: object): void {
    this.responseHandlingLocks.add(lock);
  }

  /** Resumes rendering once all locks have been removed. */
  resumeResponseHandling(lock: object): void {
    this.responseHandlingLocks.delete(lock);
    if (this.responseHandlingLocks.size === 0) {
      this.resetForceHandleTimer();
      if (!this.ordering.isEmpty()) {
        this.handlePendingMessages();
      }
    }
  }

  private resetForceHandleTimer(): void {
    if (this.forceHandleMessage !== null) {
      clearTimeout(this.forceHandleMessage);
      this.forceHandleMessage = null;
    }
  }

  private forceMessageHandling(): void {
    this.forceHandleMessage = null;
    if (this.responseHandlingLocks.size > 0) {
      console.warn('WARNING: response handling was never resumed, forcibly removing locks...');
      this.responseHandlingLocks.clear();
    } else {
      console.warn(`Gave up waiting for message ${this.ordering.getExpectedServerId()} from the server`);
    }
    if (!this.handlePendingMessages() && !this.ordering.isEmpty()) {
      // Messages remain but the next id is missing (likely lost) -> resync.
      this.ordering.clear();
      this.registry.getMessageSender().requestResynchronize();
      if (this.registry.getRequestResponseTracker().hasActiveRequest()) {
        this.registry.getRequestResponseTracker().endRequest();
      }
      this.registry.getMessageSender().resynchronize();
    }
  }

  private handlePendingMessages(): boolean {
    const index = this.ordering.findNextHandlable();
    if (index !== -1) {
      const message = this.ordering.remove(index);
      this.handleJSON(message);
      return true;
    }
    return false;
  }

  /** The last server sync id seen, or -1 before any response. */
  getLastSeenServerSyncId(): number {
    return this.ordering.getLastSeenServerSyncId();
  }

  /** The CSRF token, or the default until one is received. */
  getCsrfToken(): string {
    return this.csrfToken;
  }

  /** The push connection id, or null until received. */
  getPushId(): string | null {
    return this.pushId;
  }

  /** Whether the initial UIDL has been handled. */
  isInitialUidlHandled(): boolean {
    return this.bootstrapTime !== 0;
  }

  /** Sets a one-shot handler for the next session-expiration response. */
  setNextResponseSessionExpiredHandler(handler: (() => void) | null): void {
    this.nextResponseSessionExpiredHandler = handler;
  }
}
