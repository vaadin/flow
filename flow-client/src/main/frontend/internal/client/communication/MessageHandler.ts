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

// Implementations migrated from MessageHandler.java.
//
// The MessageHandler class below is the TS port of the rest of
// MessageHandler.java: it handles incoming UIDL messages in server-sync-id order
// (queueing out-of-order / locked messages), then applies each (constants ->
// ConstantPool, changes -> TreeChangeProcessor, executeJs, dependency loading,
// session-expired/error handling). It composes the ported MessageOrdering
// (PendingMessageQueue), TreeChangeProcessor, Reactive,
// EagerDependencyTracker, and the helpers above; everything else is a
// Registry contract.

import { assert } from '../../assert';
import type { Command } from '../Command';
import { getScheduler } from '../TrackingScheduler';
import {
  getRelativeTimeMillis,
  getRelativeTimeString,
  isEnabled,
  logBootstrapTimings,
  logTimings,
  reset as resetProfiler
} from '../Profiler';
import { redirect } from '../WidgetUtil';
import type { ApplicationConfiguration } from '../ApplicationConfiguration';
import type { ConstantPool } from '../flow/ConstantPool';
import type { Dependency, DependencyLoader, LoadMode } from '../DependencyLoader';
import type { ExecuteJavaScriptProcessor } from '../flow/ExecuteJavaScriptProcessor';
import type { LoadingIndicatorStateHandler } from './LoadingIndicatorStateHandler';
import type { MessageSender } from './MessageSender';
import type { RequestResponseTracker } from './RequestResponseTracker';
import type { ResourceLoader } from '../ResourceLoader';
import type { SystemErrorHandler } from '../SystemErrorHandler';
import type { UILifecycle } from '../UILifecycle';
import type { ValueMap } from '../ValueMap';
import { getServerId, isResynchronize, PendingMessageQueue, UNDEFINED_SYNC_ID } from './MessageOrdering';
import { ResynchronizationState } from './MessageSender';
import { runWhenEagerDependenciesLoaded } from '../EagerDependencyTracker';
import { Reactive } from '../flow/reactive/Reactive';
import { processChanges as applyTreeChanges } from '../flow/TreeChangeProcessor';
import { UIState } from '../UILifecycle';
import { Console } from '../Console';

/** Removes the link and style elements with the given dependency id. */
function removeStylesheetByIdFromDom(dependencyId: string): void {
  const elements = document.querySelectorAll(`link[data-id="${dependencyId}"], style[data-id="${dependencyId}"]`);
  for (const element of Array.from(elements)) {
    element.remove();
  }
}

/** Invokes the element's afterServerUpdate callback if it defines one. */
function callAfterServerUpdates(node: Node): void {
  const target = node as unknown as { afterServerUpdate?: () => void };
  if (node && target.afterServerUpdate) {
    target.afterServerUpdate();
  }
}

/** Milliseconds from the navigation response start to now, or -1 if unknown. */
function calculateBootstrapTime(): number {
  const perf = window.performance as (Performance & { timing?: { responseStart: number } }) | undefined;
  if (perf && perf.timing) {
    return Date.now() - perf.timing.responseStart;
  }
  return -1;
}

function parseJSONResponse(jsonText: string): ValueMap {
  return JSON.parse(jsonText) as ValueMap;
}

/** The navigation fetchStart timestamp, or 0 if unknown. */
function getFetchStartTime(): number {
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

/** The slice of Registry MessageHandler uses. */
interface MessageHandlerRegistry {
  getUILifecycle(): Pick<UILifecycle, 'getState' | 'setState'>;
  getMessageSender(): Pick<
    MessageSender,
    | 'getResynchronizationState'
    | 'clearResynchronizationState'
    | 'setClientToServerMessageId'
    | 'requestResynchronize'
    | 'resynchronize'
  >;
  getStateTree(): unknown & { prepareForResync(): void };
  getRequestResponseTracker(): Pick<
    RequestResponseTracker,
    'fireResponseHandlingStarted' | 'endRequest' | 'hasActiveRequest'
  >;
  getLoadingIndicatorStateHandler(): Pick<LoadingIndicatorStateHandler, 'stopLoading'>;
  getConstantPool(): Pick<ConstantPool, 'importFromJson'>;
  getExecuteJavaScriptProcessor(): Pick<ExecuteJavaScriptProcessor, 'execute'>;
  getDependencyLoader(): Pick<DependencyLoader, 'loadDependencies'>;
  getSystemErrorHandler(): Pick<SystemErrorHandler, 'handleSessionExpiredError' | 'handleUnrecoverableError'>;
  getApplicationConfiguration(): Pick<ApplicationConfiguration, 'getMaxMessageSuspendTimeout'>;
  getResourceLoader(): Pick<ResourceLoader, 'clearLoadedResourceById'>;
}

/** A state node whose DOM updates should be flushed after a server message. */
interface UpdatedNode {
  isUnregistered(): boolean;
  getDomNode(): Node | null;
}

/**
 * A MessageHandler is responsible for handling all incoming messages (JSON)
 * from the server (state changes, RPCs and other updates) and ensuring that the
 * connectors are updated accordingly.
 */
export class MessageHandler {
  readonly #registry: MessageHandlerRegistry;

  // Locks; while non-empty, response handling is suspended.
  readonly #responseHandlingLocks = new Set<object>();

  // The server-sync-id ordering state + the queue of pending messages.
  readonly #ordering = new PendingMessageQueue();

  #csrfToken = CSRF_TOKEN_DEFAULT_VALUE;

  #pushId: string | null = null;

  #bootstrapTime = 0;

  // Profiling timings (ms), exposed via getProfilingData / client.getProfilingData.
  protected lastProcessingTime = 0;

  protected totalProcessingTime = 0;

  #serverTimingInfo: number[] | null = null;

  #initialMessageHandled = false;

  #forceHandleMessage: ReturnType<typeof setTimeout> | null = null;

  #nextResponseSessionExpiredHandler: Command | null = null;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: MessageHandlerRegistry) {
    this.#registry = registry;
  }

  #resetForceHandleTimer(): void {
    if (this.#forceHandleMessage !== null) {
      clearTimeout(this.#forceHandleMessage);
      this.#forceHandleMessage = null;
    }
  }

  /**
   * Handles a received UIDL JSON text, parsing it, and passing it on to the
   * appropriate handlers, while logging timing information.
   *
   * @param json - The JSON to handle
   */
  handleMessage(json: ValueMap): void {
    if (getServerId(json) === -1) {
      const meta = json.meta as ValueMap | undefined;
      if (!meta || !(META_SESSION_EXPIRED in meta)) {
        Console.error(
          "Response didn't contain a server id. " +
            'Please verify that the server is up-to-date and that the response data has not been modified in transmission.'
        );
      }
    }

    let state = this.#registry.getUILifecycle().getState();
    if (state === UIState.INITIALIZING) {
      state = UIState.RUNNING;
      this.#registry.getUILifecycle().setState(state);
    }
    if (state === UIState.RUNNING) {
      this.handleJSON(json);
    } else {
      Console.warn('Ignored received message because application has already been stopped');
    }
  }

  protected handleJSON(valueMap: ValueMap): void {
    const serverId = getServerId(valueMap);
    const hasResynchronize = isResynchronize(valueMap);

    if (
      !hasResynchronize &&
      this.#registry.getMessageSender().getResynchronizationState() === ResynchronizationState.WAITING_FOR_RESPONSE
    ) {
      if (UIDL_KEY_EXECUTE in valueMap) {
        const commands = valueMap[UIDL_KEY_EXECUTE] as unknown[][];
        for (const command of commands) {
          if (command.length > 0 && command[0] === 'window.location.reload();') {
            Console.warn('Executing forced page reload while a resync request is ongoing.');
            window.location.reload();
            return;
          }
        }
      }
      Console.warn('Queueing message from the server as a resync request is ongoing.');
      this.#ordering.push(valueMap);
      return;
    }

    this.#registry.getMessageSender().clearResynchronizationState();

    if (hasResynchronize && !this.#ordering.isNextExpectedMessage(serverId)) {
      this.#ordering.setLastSeenServerSyncId(serverId - 1);
      this.#ordering.removeOld();
    }

    const locked = this.#responseHandlingLocks.size > 0;
    if (locked || !this.#ordering.isNextExpectedMessage(serverId)) {
      if (!locked) {
        if (this.#ordering.isAlreadySeen(serverId)) {
          Console.warn(`Received message with server id ${serverId} but have already seen a newer one. Ignoring it`);
          this.#endRequestIfResponse(valueMap);
          return;
        }
      }
      this.#ordering.push(valueMap);
      if (this.#forceHandleMessage === null) {
        const timeout = this.#registry.getApplicationConfiguration().getMaxMessageSuspendTimeout();
        this.#forceHandleMessage = setTimeout(() => this.#forceMessageHandling(), timeout);
      }
      return;
    }

    if (hasResynchronize) {
      // Unregister all nodes and rebuild the state tree.
      this.#registry.getStateTree().prepareForResync();
    }

    const lock = {};
    this.suspendReponseHandling(lock);

    this.#registry.getRequestResponseTracker().fireResponseHandlingStarted();
    // Client id must be updated before server id (a server-id update can trigger
    // a resync that must use the updated client id).
    if (CLIENT_TO_SERVER_ID in valueMap) {
      this.#registry
        .getMessageSender()
        .setClientToServerMessageId(valueMap[CLIENT_TO_SERVER_ID] as number, hasResynchronize);
    }
    if (serverId !== -1) {
      this.#ordering.setLastSeenServerSyncId(serverId);
    }

    if ('redirect' in valueMap) {
      const url = (valueMap.redirect as ValueMap).url as string;
      Console.debug(`redirecting to ${url}`);
      redirect(url);
      return;
    }
    if (UIDL_SECURITY_TOKEN_ID in valueMap) {
      this.#csrfToken = valueMap[UIDL_SECURITY_TOKEN_ID] as string;
    }
    if (UIDL_PUSH_ID in valueMap) {
      this.#pushId = valueMap[UIDL_PUSH_ID] as string;
    }

    this.#handleDependencies(valueMap);

    runWhenEagerDependenciesLoaded(() => this.#processMessage(valueMap, lock));
  }

  #handleDependencies(inputJson: ValueMap): void {
    const dependencies = new Map<LoadMode, Dependency[]>();
    for (const loadMode of ['INLINE', 'EAGER', 'LAZY'] as LoadMode[]) {
      if (loadMode in inputJson) {
        dependencies.set(loadMode, inputJson[loadMode] as Dependency[]);
      }
    }
    if (dependencies.size > 0) {
      this.#registry.getDependencyLoader().loadDependencies(dependencies);
    }
  }

  /**
   * Performs the actual processing of a server message when all dependencies
   * have been loaded.
   *
   * @param valueMap - the message payload
   * @param lock - the lock object for this response
   */
  #processMessage(valueMap: ValueMap, lock: object): void {
    assert(
      getServerId(valueMap) === UNDEFINED_SYNC_ID || getServerId(valueMap) === this.getLastSeenServerSyncId(),
      'Message being processed is neither unversioned nor the last seen one'
    );

    const start = performance.now();
    if ('timings' in valueMap) {
      this.#serverTimingInfo = valueMap.timings as number[];
    }
    try {
      if ('constants' in valueMap) {
        this.#registry.getConstantPool().importFromJson(valueMap.constants as Record<string, unknown>);
      }
      if ('changes' in valueMap) {
        this.#processChanges(valueMap);
      }
      if ('stylesheetRemovals' in valueMap) {
        this.#processStylesheetRemovals(valueMap.stylesheetRemovals as string[] | null);
      }
      if (UIDL_KEY_EXECUTE in valueMap) {
        // Invoke JS only after all tree changes and post-flush listeners added
        // during message processing (hence the doubly-nested post-flush).
        Reactive.addPostFlushListener(() =>
          Reactive.addPostFlushListener(() =>
            this.#registry.getExecuteJavaScriptProcessor().execute(valueMap[UIDL_KEY_EXECUTE] as unknown[][])
          )
        );
      }

      Reactive.flush();

      const meta = valueMap.meta as ValueMap | undefined;
      if (meta) {
        if (META_SESSION_EXPIRED in meta) {
          if (this.#nextResponseSessionExpiredHandler !== null) {
            this.#nextResponseSessionExpiredHandler();
          } else if (this.#registry.getUILifecycle().getState() !== UIState.TERMINATED) {
            this.#registry.getUILifecycle().setState(UIState.TERMINATED);
            // Delay so a pending redirect/reload is not cancelled.
            setTimeout(
              () => this.#registry.getSystemErrorHandler().handleSessionExpiredError(null),
              SESSION_EXPIRED_HANDLING_DELAY
            );
          }
        } else if ('appError' in meta && this.#registry.getUILifecycle().getState() !== UIState.TERMINATED) {
          const error = meta.appError as ValueMap;
          this.#registry
            .getSystemErrorHandler()
            .handleUnrecoverableError(
              error.caption as string,
              error.message as string,
              error.details as string,
              error.url as string,
              error.querySelector as string | null
            );
          this.#registry.getUILifecycle().setState(UIState.TERMINATED);
        }
      }
      this.#nextResponseSessionExpiredHandler = null;
    } finally {
      // Mark the initial UIDL handled and end the request in finally so the UI
      // settles (ApplicationConnection.isActive returns false) even if applying
      // the message threw. In GWT the equivalent work ran inside $entry, so an
      // uncaught error never left the client perpetually "active"; here we
      // guarantee the same by not gating these on successful processing.
      this.lastProcessingTime = Math.round(performance.now() - start);
      this.totalProcessingTime += this.lastProcessingTime;
      if (!this.#initialMessageHandled) {
        this.#initialMessageHandled = true;

        const fetchStart = getFetchStartTime();
        if (fetchStart !== 0) {
          const time = Math.round(Date.now() - fetchStart);
          Console.debug(`First response processed ${time} ms after fetchStart`);
        }

        this.#bootstrapTime = calculateBootstrapTime();
        if (isEnabled() && this.#bootstrapTime !== -1) {
          logBootstrapTimings();
        }
      }

      Console.debug(` Processing time was ${this.lastProcessingTime}ms`);

      this.#endRequestIfResponse(valueMap);
      this.resumeResponseHandling(lock);

      if (isEnabled()) {
        getScheduler().scheduleDeferred(() => {
          logTimings();
          resetProfiler();
        });
      }
    }
  }

  #processStylesheetRemovals(removals: string[] | null): void {
    if (removals === null || removals.length === 0) {
      return;
    }

    Console.debug(`Processing ${removals.length} stylesheet removals`);

    for (const dependencyId of removals) {
      this.#removeStylesheetById(dependencyId);
    }
  }

  #removeStylesheetById(dependencyId: string): void {
    removeStylesheetByIdFromDom(dependencyId);
    this.#registry.getResourceLoader().clearLoadedResourceById(dependencyId);
  }

  #processChanges(json: ValueMap): void {
    const tree = this.#registry.getStateTree();
    // Error/meta responses (e.g. an unrecoverable error) carry "changes":{} — an
    // empty object, not an array. GWT's JsonArray.length() treated that as zero
    // changes; here a non-array would make `for...of` throw and abort before the
    // meta.appError handling runs, so coerce it to an empty list.
    const changes = Array.isArray(json.changes) ? (json.changes as Array<Record<string, unknown>>) : [];
    // The StateTree satisfies TreeChangeProcessor's contract.
    const updatedNodes = applyTreeChanges(tree as never, changes);
    Reactive.addPostFlushListener(() =>
      // Through the tracking scheduler, as Scheduler.get().scheduleDeferred is,
      // so the pending callbacks keep the application active.
      getScheduler().scheduleDeferred(() =>
        updatedNodes.forEach((node) => this.#afterServerUpdates(node as unknown as UpdatedNode))
      )
    );
  }

  #afterServerUpdates(node: UpdatedNode): void {
    if (!node.isUnregistered()) {
      const domNode = node.getDomNode();
      if (domNode) {
        callAfterServerUpdates(domNode);
      }
    }
  }

  #endRequestIfResponse(json: ValueMap): void {
    if (this.#isResponse(json)) {
      this.#registry.getRequestResponseTracker().endRequest();
      this.#registry.getLoadingIndicatorStateHandler().stopLoading();
    }
  }

  #isResponse(json: ValueMap): boolean {
    const meta = json.meta as ValueMap | undefined;
    return !meta || !(META_ASYNC in meta);
  }

  #forceMessageHandling(): void {
    this.#forceHandleMessage = null;
    if (this.#responseHandlingLocks.size > 0) {
      Console.warn('WARNING: response handling was never resumed, forcibly removing locks...');
      this.#responseHandlingLocks.clear();
    } else {
      Console.warn(`Gave up waiting for message ${this.#ordering.getExpectedServerId()} from the server`);
    }
    if (!this.#handlePendingMessages() && !this.#ordering.isEmpty()) {
      // Messages remain but the next id is missing (likely lost) -> resync.
      this.#ordering.clear();
      this.#registry.getMessageSender().requestResynchronize();
      if (this.#registry.getRequestResponseTracker().hasActiveRequest()) {
        this.#registry.getRequestResponseTracker().endRequest();
      }
      this.#registry.getMessageSender().resynchronize();
    }
  }

  /**
   * This method can be used to postpone rendering of a response for a short
   * period of time (e.g. to avoid the rendering process during animation).
   *
   * The Java method name is misspelled; it is kept verbatim to preserve public
   * API parity.
   *
   * @param lock - the lock
   */
  suspendReponseHandling(lock: object): void {
    this.#responseHandlingLocks.add(lock);
  }

  /**
   * Resumes the rendering process once all locks have been removed.
   *
   * @param lock - the lock
   */
  resumeResponseHandling(lock: object): void {
    this.#responseHandlingLocks.delete(lock);
    if (this.#responseHandlingLocks.size === 0) {
      this.#resetForceHandleTimer();
      if (!this.#ordering.isEmpty()) {
        this.#handlePendingMessages();
      }
    }
  }

  /**
   * Finds the next pending UIDL message and handles it (next pending is decided
   * based on the server id).
   *
   * @returns true if a message was handled, false otherwise
   */
  #handlePendingMessages(): boolean {
    const index = this.#ordering.findNextHandlable();
    if (index !== -1) {
      const message = this.#ordering.remove(index);
      this.handleJSON(message);
      return true;
    }
    return false;
  }

  /**
   * Profiling data for the last response: last and total processing time, the
   * optional server timing info, and the bootstrap time. Mirrors the
   * getProfilingData JSNI in ApplicationConnection.java.
   */
  getProfilingData(): number[] {
    const data = [this.lastProcessingTime, this.totalProcessingTime];
    if (this.#serverTimingInfo !== null) {
      data.push(...this.#serverTimingInfo);
    } else {
      data.push(-1, -1);
    }
    data.push(this.#bootstrapTime);
    return data;
  }

  /**
   * Gets the server id included in the last received response.
   *
   * This id can be used by connectors to determine whether new data has been
   * received from the server to avoid doing the same calculations multiple times.
   *
   * No guarantees are made for the structure of the id other than that there will
   * be a new unique value every time a new response with data from the server is
   * received.
   *
   * The initial id when no request has yet been processed is -1.
   *
   * @returns an id identifying the response
   */
  getLastSeenServerSyncId(): number {
    return this.#ordering.getLastSeenServerSyncId();
  }

  /**
   * Gets the token (synchronizer token pattern) that the server uses to protect against
   * CSRF (Cross Site Request Forgery) attacks.
   *
   * @returns the CSRF token string
   */
  getCsrfToken(): string {
    return this.#csrfToken;
  }

  /**
   * Gets the push connection identifier for this session. Used when establishing a push
   * connection with the client.
   *
   * @returns the push connection identifier string
   */
  getPushId(): string | null {
    return this.#pushId;
  }

  /**
   * Checks if the first UIDL has been handled.
   *
   * @returns true if the initial UIDL has already been processed, false * otherwise
   */
  isInitialUidlHandled(): boolean {
    return this.#bootstrapTime !== 0;
  }

  /**
   * Sets a temporary handler for session expiration. This handler will be triggered if
   * and only if the next server message tells that the session has expired.
   *
   * @param handler - the handler to use or null to remove a previously set handler
   */
  setNextResponseSessionExpiredHandler(handler: (() => void) | null): void {
    this.#nextResponseSessionExpiredHandler = handler;
  }
}

// Java declares parseJson between isInitialUidlHandled and
// setNextResponseSessionExpiredHandler; a module function cannot live inside the
// class body, so it follows it here.
/**
 * Parse the given wrapped JSON, received from the server, to a ValueMap.
 *
 * @param jsonText - The JSON to parse
 * @returns A ValueMap created from the JSON
 */
export function parseJson(jsonText: string | null): ValueMap | null {
  if (jsonText === null) {
    return null;
  }
  const start = getRelativeTimeMillis();
  try {
    const json = parseJSONResponse(jsonText);
    Console.debug(`JSON parsing took ${getRelativeTimeString(start)}ms`);
    return json;
  } catch {
    Console.error(`Unable to parse JSON: ${jsonText}`);
    return null;
  }
}
