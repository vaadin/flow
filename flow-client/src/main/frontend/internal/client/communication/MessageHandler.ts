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
import { DependencyLoader } from '../DependencyLoader';
import { Profiler } from '../Profiler';
import { Reactive } from '../flow/reactive/Reactive';
import { TreeChangeProcessor } from '../flow/TreeChangeProcessor';

// Mirrors ApplicationConstants. Strings are inlined so the TS module doesn't
// need to take a flow-shared bridge dependency.
const CSRF_TOKEN_DEFAULT_VALUE = 'init';
const SERVER_SYNC_ID = 'syncId';
const CLIENT_TO_SERVER_ID = 'clientId';
const RESYNCHRONIZE_ID = 'resynchronize';
const UIDL_SECURITY_TOKEN_ID = 'Vaadin-Security-Key';
const UIDL_PUSH_ID = 'Vaadin-Push-ID';

// Mirrors JsonConstants.
const META_ASYNC = 'async';
const META_SESSION_EXPIRED = 'sessionExpired';
const UIDL_KEY_EXECUTE = 'execute';

// Mirrors LoadMode enum values; iterated when partitioning dependencies by
// load mode (kept in sync with com.vaadin.flow.shared.ui.LoadMode).
const LOAD_MODES = ['EAGER', 'LAZY', 'INLINE'] as const;

/** The value of an undefined sync id; matches the Java constant. */
const UNDEFINED_SYNC_ID = -1;

interface AfterServerUpdateNode {
  afterServerUpdate?: () => void;
}

/** Generic JSON value shape. */
type JsonObject = Record<string, unknown>;

/** Minimal UILifecycle shape used by this module. */
interface UILifecycleLike {
  isRunning(): boolean;
  getStateName(): string;
  setStateName(state: string): void;
}

/** Minimal MessageSender shape used by this module (the sibling TS class). */
interface MessageSenderLike {
  getResynchronizationState(): string;
  clearResynchronizationState(): void;
  setClientToServerMessageId(nextExpectedId: number, force: boolean): void;
  requestResynchronize(): boolean;
  resynchronize(): void;
}

/** Minimal RequestResponseTracker shape used by this module. */
interface RequestResponseTrackerLike {
  hasActiveRequest(): boolean;
  endRequest(): void;
  fireResponseHandlingStarted(): void;
}

/** Minimal LoadingIndicatorStateHandler shape used by this module. */
interface LoadingIndicatorStateHandlerLike {
  stopLoading(): void;
}

/** Minimal ApplicationConfiguration shape; matches the TS class properties. */
interface ApplicationConfigurationLike {
  productionMode: boolean;
  maxMessageSuspendTimeout: number;
}

/** Minimal StateTree shape used by this module. */
interface StateTreeLike {
  prepareForResync(): void;
  getRootNode(): { getDebugJson(): unknown };
}

/** Minimal ConstantPool shape used by this module. */
interface ConstantPoolLike {
  importFromJson(json: JsonObject): void;
}

/** Minimal SystemErrorHandler shape used by this module. */
interface SystemErrorHandlerLike {
  handleSessionExpiredError(details: string | null): void;
  handleUnrecoverableError(
    caption: string | null,
    message: string | null,
    details: string | null,
    url: string | null,
    querySelector: string | null
  ): void;
}

/** Minimal ResourceLoader shape used by this module. */
interface ResourceLoaderLike {
  clearLoadedResourceById(dependencyId: string): void;
}

/** Minimal DependencyLoader shape used by this module. */
interface DependencyLoaderLike {
  requireHtmlImportsReady(): void;
  loadDependencies(deps: Map<string, unknown[]>): void;
}

/** Minimal ExecuteJavaScriptProcessor shape used by this module. */
interface ExecuteJavaScriptProcessorLike {
  execute(invocations: unknown[]): void;
}

/**
 * Wiring required by {@link MessageHandler}: dependencies that may need lazy
 * resolution (sibling MessageSender, the registry-reset hook) and direct
 * service references for everything that is already constructed by the time
 * MessageHandler is created.
 */
export interface MessageHandlerCallbacks {
  getMessageSender(): MessageSenderLike;
  getUiLifecycle(): UILifecycleLike;
  getStateTree(): StateTreeLike;
  getConstantPool(): ConstantPoolLike;
  getSystemErrorHandler(): SystemErrorHandlerLike;
  getExecuteJavaScriptProcessor(): ExecuteJavaScriptProcessorLike;
  getDependencyLoader(): DependencyLoaderLike;
  getResourceLoader(): ResourceLoaderLike;
  getRequestResponseTracker(): RequestResponseTrackerLike;
  getLoadingIndicatorStateHandler(): LoadingIndicatorStateHandlerLike;
  getApplicationConfiguration(): ApplicationConfigurationLike;
  redirect(url: string): void;
}

/**
 * Data structure holding information about pending UIDL messages.
 */
class PendingUIDLMessage {
  readonly json: JsonObject;

  constructor(json: JsonObject) {
    this.json = json;
  }

  getJson(): JsonObject {
    return this.json;
  }
}

function hasKey(obj: JsonObject | null | undefined, key: string): boolean {
  return obj != null && Object.prototype.hasOwnProperty.call(obj, key);
}

function getString(obj: JsonObject, key: string): string {
  return obj[key] as string;
}

function getNumber(obj: JsonObject, key: string): number {
  return obj[key] as number;
}

function getObject(obj: JsonObject, key: string): JsonObject {
  return obj[key] as JsonObject;
}

function getArray(obj: JsonObject, key: string): unknown[] {
  return obj[key] as unknown[];
}

/**
 * Handles all incoming messages (JSON) from the server (state changes, RPCs
 * and other updates) and ensures connectors are updated accordingly. Migrated
 * from `com.vaadin.client.communication.MessageHandler`.
 *
 * Wiring (sibling MessageSender, the registry-reset hook and the various
 * services) is delivered through {@link MessageHandlerCallbacks}.
 */
export class MessageHandler {
  static removeStylesheetByIdFromDom(dependencyId: string): void {
    const selector = `link[data-id="${dependencyId}"], style[data-id="${dependencyId}"]`;
    for (const el of Array.from(document.querySelectorAll(selector))) {
      el.remove();
    }
  }

  static callAfterServerUpdates(node: Node | null): void {
    const candidate = node as AfterServerUpdateNode | null;
    if (candidate && typeof candidate.afterServerUpdate === 'function') {
      candidate.afterServerUpdate();
    }
  }

  static calculateBootstrapTime(): number {
    const timing = performance?.timing;
    if (timing) {
      return Date.now() - timing.responseStart;
    }
    return -1;
  }

  /**
   * Parses the given JSON from the server.
   * @returns a parsed object or null if the input could not be parsed
   */
  static parseJson(jsonText: string | null): JsonObject | null {
    if (jsonText == null) {
      return null;
    }
    const start = Profiler.getRelativeTimeMillis();
    try {
      const json = JSON.parse(jsonText) as JsonObject;
      Console.debug(`JSON parsing took ${Profiler.getRelativeTimeString(start)}ms`);
      return json;
    } catch {
      Console.error(`Unable to parse JSON: ${jsonText}`);
      return null;
    }
  }

  private static getFetchStartTime(): number {
    return performance?.timing?.fetchStart ?? 0;
  }

  private readonly callbacks: MessageHandlerCallbacks;
  // If responseHandlingLocks contains any entries, response handling is
  // suspended until the set is empty or a timeout fires.
  private readonly responseHandlingLocks = new Set<unknown>();
  // UIDL messages received while response handling is suspended.
  private pendingUIDLMessages: PendingUIDLMessage[] = [];
  private csrfToken = CSRF_TOKEN_DEFAULT_VALUE;
  private pushId: string | null = null;
  private lastProcessingTime = 0;
  private totalProcessingTime = 0;
  // -2 means not yet calculated. Also used to track whether the first UIDL
  // has been handled (isInitialUidlHandled).
  private bootstrapTime = 0;
  private serverTimingInfo: JsonObject | null = null;
  private lastSeenServerSyncId = UNDEFINED_SYNC_ID;
  private initialMessageHandled = false;
  // Timer that breaks the response-handling lock if a misbehaving component
  // never releases it.
  private forceHandleMessageTimer: ReturnType<typeof setTimeout> | null = null;
  private nextResponseSessionExpiredHandler: (() => void) | null = null;

  constructor(callbacks: MessageHandlerCallbacks) {
    this.callbacks = callbacks;
  }

  /** Handles a received UIDL JSON object. */
  handleMessage(json: JsonObject | null): void {
    if (json == null) {
      throw new Error('The json to handle cannot be null');
    }
    if (this.getServerId(json) === -1) {
      const meta = (json.meta as JsonObject | undefined) ?? null;
      if (meta == null || !hasKey(meta, META_SESSION_EXPIRED)) {
        Console.error(
          'Response did' +
            "n't contain a server id. " +
            'Please verify that the server is up-to-date and that the response data has not been modified in transmission.'
        );
      }
    }

    const lifecycle = this.callbacks.getUiLifecycle();
    let state = lifecycle.getStateName();
    if (state === 'INITIALIZING') {
      // Application starting up for the first time.
      lifecycle.setStateName('RUNNING');
      state = 'RUNNING';
    }

    if (state === 'RUNNING') {
      this.handleJSON(json);
    } else {
      Console.warn('Ignored received message because application has already been stopped');
    }
  }

  protected handleJSON(valueMap: JsonObject): void {
    const serverId = this.getServerId(valueMap);
    const hasResynchronize = this.isResynchronize(valueMap);

    const messageSender = this.callbacks.getMessageSender();
    if (!hasResynchronize && messageSender.getResynchronizationState() === 'WAITING_FOR_RESPONSE') {
      if (hasKey(valueMap, UIDL_KEY_EXECUTE)) {
        const commands = getArray(valueMap, UIDL_KEY_EXECUTE);
        for (const cmd of commands) {
          const command = cmd as unknown[];
          if (command.length > 0 && command[0] === 'window.location.reload();') {
            Console.warn('Executing forced page reload while a resync request is ongoing.');
            window.location.reload();
            return;
          }
        }
      }

      // A resync is in progress (WAITING_FOR_RESPONSE state). The incoming
      // message could have been generated by a background thread during
      // server-side resync and pushed to the client, so it's a potentially
      // valid message that should be processed after resync completes.
      // Queue this message; if its id is older than the resync request, it
      // will be discarded during subsequent processing.
      Console.warn('Queueing message from the server as a resync request is ongoing.');
      this.pendingUIDLMessages.push(new PendingUIDLMessage(valueMap));
      return;
    }

    messageSender.clearResynchronizationState();

    if (hasResynchronize && !this.isNextExpectedMessage(serverId)) {
      // Resynchronize request. We must remove any old pending messages and
      // ensure this is handled next. Otherwise we would keep waiting for
      // an older message forever (if this is triggered by forceMessageHandling).
      Console.debug(`Received resync message with id ${serverId} while waiting for ${this.getExpectedServerId()}`);
      this.lastSeenServerSyncId = serverId - 1;
      this.removeOldPendingMessages();
    }

    const locked = this.responseHandlingLocks.size > 0;

    if (locked || !this.isNextExpectedMessage(serverId)) {
      if (locked) {
        // Some component is doing something that can't be interrupted
        // (e.g. animation that should be smooth). Enqueue for later.
        Console.debug('Postponing UIDL handling due to lock...');
      } else {
        // Unexpected server id.
        if (serverId <= this.lastSeenServerSyncId) {
          // Why is the server re-sending an old package? Ignore it.
          Console.warn(
            `Received message with server id ${serverId} but have already seen ${this.lastSeenServerSyncId}. Ignoring it`
          );
          this.endRequestIfResponse(valueMap);
          return;
        }

        Console.debug(
          `Received message with server id ${serverId} but expected ${this.getExpectedServerId()}. Postponing handling until the missing message(s) have been received`
        );
      }
      this.pendingUIDLMessages.push(new PendingUIDLMessage(valueMap));
      if (this.forceHandleMessageTimer === null) {
        const timeout = this.callbacks.getApplicationConfiguration().maxMessageSuspendTimeout;
        this.forceHandleMessageTimer = setTimeout(() => {
          this.forceHandleMessageTimer = null;
          this.forceMessageHandling();
        }, timeout);
      }
      return;
    }

    // Should only prepare resync after the (locked || !isNextExpectedMessage)
    // check above. prepareForResync removes the nodes, and if locked is true
    // we return without handling the message — so nodes wouldn't be added
    // back. Related to https://github.com/vaadin/flow/issues/8699 — the root
    // cause seems to be that `connectClient` is removed from rootNode (<body>)
    // during a resync and not added back.
    if (this.isResynchronize(valueMap)) {
      this.callbacks.getStateTree().prepareForResync();
    }

    const start = Date.now();
    // Lock response handling to avoid a situation where something pushed
    // from the server gets processed while waiting for e.g. lazily loaded
    // connectors that are needed for processing the current message.
    const lock = {};
    this.suspendReponseHandling(lock);

    Console.debug('Handling message from server');
    this.callbacks.getRequestResponseTracker().fireResponseHandlingStarted();
    // Client id must be updated before server id, as server id update can
    // cause a resync (which must use the updated id).
    if (hasKey(valueMap, CLIENT_TO_SERVER_ID)) {
      const serverNextExpected = getNumber(valueMap, CLIENT_TO_SERVER_ID);
      messageSender.setClientToServerMessageId(serverNextExpected, this.isResynchronize(valueMap));
    }

    if (serverId !== -1) {
      // Use sync id unless explicitly set as undefined, as is done by e.g.
      // critical server-side notifications.
      this.lastSeenServerSyncId = serverId;
    }

    // Handle redirect.
    if (hasKey(valueMap, 'redirect')) {
      const url = getString(getObject(valueMap, 'redirect'), 'url');
      Console.debug(`redirecting to ${url}`);
      this.callbacks.redirect(url);
      return;
    }

    // Get security key.
    if (hasKey(valueMap, UIDL_SECURITY_TOKEN_ID)) {
      this.csrfToken = getString(valueMap, UIDL_SECURITY_TOKEN_ID);
    }

    // Get push id if present.
    if (hasKey(valueMap, UIDL_PUSH_ID)) {
      this.pushId = getString(valueMap, UIDL_PUSH_ID);
    }

    this.handleDependencies(valueMap);

    if (!this.initialMessageHandled) {
      // When handling the initial JSON message, dependencies are embedded in
      // the HTML document instead of being injected by DependencyLoader. We
      // must still explicitly wait for all HTML imports from the HTML
      // document to be loaded; JavaScript dependencies are handled by the
      // browser automatically.
      this.callbacks.getDependencyLoader().requireHtmlImportsReady();
    }

    // Hook for e.g. TestBench to get details about server performance.
    if (hasKey(valueMap, 'timings')) {
      this.serverTimingInfo = getObject(valueMap, 'timings');
    }

    DependencyLoader.runWhenEagerDependenciesLoaded(() => this.processMessage(valueMap, lock, start));
  }

  private handleDependencies(inputJson: JsonObject): void {
    Console.debug('Handling dependencies');
    const dependencies = new Map<string, unknown[]>();
    for (const loadMode of LOAD_MODES) {
      if (hasKey(inputJson, loadMode)) {
        dependencies.set(loadMode, getArray(inputJson, loadMode));
      }
    }

    if (dependencies.size > 0) {
      this.callbacks.getDependencyLoader().loadDependencies(dependencies);
    }
  }

  /**
   * Performs the actual processing of a server message when all dependencies
   * have been loaded.
   */
  private processMessage(valueMap: JsonObject, lock: object, start: number): void {
    try {
      const processUidlStart = Date.now();

      if (hasKey(valueMap, 'constants')) {
        const constants = getObject(valueMap, 'constants');
        this.callbacks.getConstantPool().importFromJson(constants);
      }

      if (hasKey(valueMap, 'changes')) {
        this.processChanges(valueMap);
      }

      if (hasKey(valueMap, 'stylesheetRemovals')) {
        this.processStylesheetRemovals(getArray(valueMap, 'stylesheetRemovals') as string[]);
      }

      if (hasKey(valueMap, UIDL_KEY_EXECUTE)) {
        // Invoke JS only after all tree changes have been propagated and
        // after post-flush listeners added during message processing (so add
        // one more post-flush listener which is called after all added
        // post-flush listeners).
        Reactive.addPostFlushListener(() =>
          Reactive.addPostFlushListener(() =>
            this.callbacks.getExecuteJavaScriptProcessor().execute(getArray(valueMap, UIDL_KEY_EXECUTE))
          )
        );
      }

      Console.debug(`handleUIDLMessage: ${Date.now() - processUidlStart} ms`);

      Reactive.flush();

      const meta = (valueMap.meta as JsonObject | undefined) ?? null;

      if (meta != null) {
        const lifecycle = this.callbacks.getUiLifecycle();
        const uiState = lifecycle.getStateName();
        if (hasKey(meta, META_SESSION_EXPIRED)) {
          if (this.nextResponseSessionExpiredHandler !== null) {
            this.nextResponseSessionExpiredHandler();
          } else if (uiState !== 'TERMINATED') {
            lifecycle.setStateName('TERMINATED');
            // Delay session expiration handling to prevent cancelling
            // potential ongoing page redirect/reload.
            setTimeout(() => {
              this.callbacks.getSystemErrorHandler().handleSessionExpiredError(null);
            }, 250);
          }
        } else if (hasKey(meta, 'appError') && uiState !== 'TERMINATED') {
          const error = getObject(meta, 'appError');
          this.callbacks
            .getSystemErrorHandler()
            .handleUnrecoverableError(
              getString(error, 'caption'),
              getString(error, 'message'),
              getString(error, 'details'),
              getString(error, 'url'),
              getString(error, 'querySelector')
            );
          lifecycle.setStateName('TERMINATED');
        }
      }
      this.nextResponseSessionExpiredHandler = null;

      this.lastProcessingTime = Math.trunc(Date.now() - start);
      this.totalProcessingTime += this.lastProcessingTime;
      if (!this.initialMessageHandled) {
        this.initialMessageHandled = true;

        const fetchStart = MessageHandler.getFetchStartTime();
        if (fetchStart !== 0) {
          const time = Math.trunc(Date.now() - fetchStart);
          Console.debug(`First response processed ${time} ms after fetchStart`);
        }

        this.bootstrapTime = MessageHandler.calculateBootstrapTime();
      }
    } finally {
      Console.debug(` Processing time was ${this.lastProcessingTime}ms`);

      this.endRequestIfResponse(valueMap);
      this.resumeResponseHandling(lock);
    }
  }

  private processStylesheetRemovals(removals: string[]): void {
    if (removals == null || removals.length === 0) {
      return;
    }

    Console.debug(`Processing ${removals.length} stylesheet removals`);

    for (const dependencyId of removals) {
      MessageHandler.removeStylesheetByIdFromDom(dependencyId);
      this.callbacks.getResourceLoader().clearLoadedResourceById(dependencyId);
    }
  }

  private processChanges(json: JsonObject): void {
    const tree = this.callbacks.getStateTree();
    const updatedNodes = TreeChangeProcessor.processChanges(
      tree as unknown as Parameters<typeof TreeChangeProcessor.processChanges>[0],
      getArray(json, 'changes') as Parameters<typeof TreeChangeProcessor.processChanges>[1]
    );

    if (!this.callbacks.getApplicationConfiguration().productionMode) {
      try {
        const debugJson = tree.getRootNode().getDebugJson();
        Console.debug('StateTree after applying changes:');
        Console.debug(debugJson);
      } catch (e) {
        Console.error('Failed to log state tree');
        Console.error(e);
      }
    }

    Reactive.addPostFlushListener(() =>
      setTimeout(() => {
        updatedNodes.forEach((node) => this.afterServerUpdates(node));
      }, 0)
    );
  }

  private afterServerUpdates(node: unknown): void {
    const stateNode = node as { isUnregistered(): boolean; getDomNode(): Node | null };
    if (!stateNode.isUnregistered()) {
      MessageHandler.callAfterServerUpdates(stateNode.getDomNode());
    }
  }

  private endRequestIfResponse(json: JsonObject): void {
    if (this.isResponse(json)) {
      // End the request if the received message was a response, not sent
      // asynchronously.
      this.callbacks.getRequestResponseTracker().endRequest();
      this.callbacks.getLoadingIndicatorStateHandler().stopLoading();
    }
  }

  private isResynchronize(json: JsonObject): boolean {
    return hasKey(json, RESYNCHRONIZE_ID);
  }

  private isResponse(json: JsonObject): boolean {
    const meta = (json.meta as JsonObject | undefined) ?? null;
    if (meta == null || !hasKey(meta, META_ASYNC)) {
      return true;
    }
    return false;
  }

  /** Checks if the given serverId is the one we are currently waiting for. */
  private isNextExpectedMessage(serverId: number): boolean {
    if (serverId === -1) {
      return true;
    }
    if (serverId === this.getExpectedServerId()) {
      return true;
    }
    if (this.lastSeenServerSyncId === UNDEFINED_SYNC_ID) {
      // First message is always ok.
      return true;
    }
    return false;
  }

  private getServerId(json: JsonObject): number {
    if (hasKey(json, SERVER_SYNC_ID)) {
      return getNumber(json, SERVER_SYNC_ID);
    }
    return -1;
  }

  private getExpectedServerId(): number {
    return this.lastSeenServerSyncId + 1;
  }

  private forceMessageHandling(): void {
    if (this.responseHandlingLocks.size > 0) {
      // Lock which was never released -> bug in locker or things just too slow.
      Console.warn('WARNING: reponse handling was never resumed, forcibly removing locks...');
      this.responseHandlingLocks.clear();
    } else {
      // Waited for out-of-order message which never arrived. Do one final
      // check and resynchronize if the message is not there.
      Console.warn(`Gave up waiting for message ${this.getExpectedServerId()} from the server`);
    }
    if (!this.handlePendingMessages() && this.pendingUIDLMessages.length > 0) {
      // There are messages but the next id was not found, likely it has
      // been lost. Drop pending messages and resynchronize.
      this.pendingUIDLMessages = [];

      const messageSender = this.callbacks.getMessageSender();
      // Inform the message sender that resynchronize is desired already
      // since endRequest may already send out a next request.
      messageSender.requestResynchronize();

      // Clear previous request if it exists.
      if (this.callbacks.getRequestResponseTracker().hasActiveRequest()) {
        this.callbacks.getRequestResponseTracker().endRequest();
      }

      // Call resynchronize to make sure a resynchronize request is sent in
      // case endRequest did not already do this.
      messageSender.resynchronize();
    }
  }

  /** Postpones rendering of a response for a short period (e.g. animations). */
  suspendReponseHandling(lock: unknown): void {
    this.responseHandlingLocks.add(lock);
  }

  /** Resumes the rendering process once all locks have been removed. */
  resumeResponseHandling(lock: unknown): void {
    this.responseHandlingLocks.delete(lock);
    if (this.responseHandlingLocks.size === 0) {
      // Cancel the timer that breaks the lock.
      if (this.forceHandleMessageTimer !== null) {
        clearTimeout(this.forceHandleMessageTimer);
        this.forceHandleMessageTimer = null;
      }

      if (this.pendingUIDLMessages.length > 0) {
        Console.debug('No more response handling locks, handling pending requests.');
        this.handlePendingMessages();
      }
    }
  }

  /** Finds the next pending UIDL message (by server id) and handles it. */
  private handlePendingMessages(): boolean {
    if (this.pendingUIDLMessages.length === 0) {
      return false;
    }

    // Find the next expected message.
    let toHandle = -1;
    for (let i = 0; i < this.pendingUIDLMessages.length; i++) {
      const message = this.pendingUIDLMessages[i];
      if (this.isNextExpectedMessage(this.getServerId(message.json))) {
        toHandle = i;
        break;
      }
    }

    if (toHandle !== -1) {
      const messageToHandle = this.pendingUIDLMessages.splice(toHandle, 1)[0];
      this.handleJSON(messageToHandle.getJson());
      // Any remaining messages will be handled when this is called again
      // at the end of handleJSON.
      return true;
    }
    return false;
  }

  private removeOldPendingMessages(): void {
    for (let i = 0; i < this.pendingUIDLMessages.length; i++) {
      const m = this.pendingUIDLMessages[i];
      const serverId = this.getServerId(m.json);
      if (serverId !== -1 && serverId < this.getExpectedServerId()) {
        Console.debug(`Removing old message with id ${serverId}`);
        this.pendingUIDLMessages.splice(i, 1);
        i--;
      }
    }
  }

  /** Gets the server id included in the last received response. */
  getLastSeenServerSyncId(): number {
    return this.lastSeenServerSyncId;
  }

  /**
   * Returns the profiling data array exposed on the client as
   * {@code client.getProfilingData()}: {@code [lastProcessingTime,
   * totalProcessingTime, serverTimingInfo[0], serverTimingInfo[1],
   * bootstrapTime]}. When server-side timings are unavailable, both
   * server-side slots default to -1 so the array shape stays at 5 entries.
   */
  getProfilingData(): unknown[] {
    const data: unknown[] = [];
    data.push(this.lastProcessingTime);
    data.push(this.totalProcessingTime);
    if (Array.isArray(this.serverTimingInfo)) {
      data.push((this.serverTimingInfo as unknown[])[0]);
      data.push((this.serverTimingInfo as unknown[])[1]);
    } else {
      data.push(-1);
      data.push(-1);
    }
    data.push(this.bootstrapTime);
    return data;
  }

  /** Gets the CSRF token. */
  getCsrfToken(): string {
    return this.csrfToken;
  }

  /** Gets the push connection identifier for this session. */
  getPushId(): string | null {
    return this.pushId;
  }

  /** Checks if the first UIDL has been handled. */
  isInitialUidlHandled(): boolean {
    return this.bootstrapTime !== 0;
  }

  /** Sets a temporary handler for session expiration on the next response. */
  setNextResponseSessionExpiredHandler(handler: (() => void) | null): void {
    this.nextResponseSessionExpiredHandler = handler;
  }
}
