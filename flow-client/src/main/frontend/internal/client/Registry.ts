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

import type { MessageSender } from './communication/MessageSender';
import type { MessageHandler } from './communication/MessageHandler';
import type { ApplicationConnection } from './ApplicationConnection';
import type { Heartbeat } from './communication/Heartbeat';
import type { ConnectionStateHandler } from './communication/ConnectionStateHandler';
import type { ServerRpcQueue } from './communication/ServerRpcQueue';
import type { ApplicationConfiguration } from './ApplicationConfiguration';
import type { StateTree } from './flow/StateTree';
import type { PushConfiguration } from './communication/PushConfiguration';
import type { XhrConnection } from './communication/XhrConnection';
import type { URIResolver } from './URIResolver';
import type { DependencyLoader } from './DependencyLoader';
import type { SystemErrorHandler } from './SystemErrorHandler';
import type { UILifecycle } from './UILifecycle';
import type { RequestResponseTracker } from './communication/RequestResponseTracker';
import type { ReconnectConfiguration } from './communication/ReconnectConfiguration';
import type { ExecuteJavaScriptProcessor } from './flow/ExecuteJavaScriptProcessor';
import type { ServerConnector } from './communication/ServerConnector';
import type { ResourceLoader } from './ResourceLoader';
import type { ConstantPool } from './flow/ConstantPool';
import type { ExistingElementMap } from './ExistingElementMap';
import type { InitialPropertiesHandler } from './InitialPropertiesHandler';
import type { Poller } from './communication/Poller';
import type { LoadingIndicatorStateHandler } from './communication/LoadingIndicatorStateHandler';
import { assert } from '../assert';

// TypeScript port of the container mechanism of com.vaadin.client.Registry.
// The Registry is a holder of singleton services (MessageSender, StateTree, ...)
// looked up by a key; some are resettable via a supplier so the whole set can be
// recreated. The original Java class keyed by Class<?>; here a service is keyed
// by an opaque token (a string, symbol or constructor) since some TS services
// are functions, not classes.
//
// The 24 typed getters are declared here, as in Registry.java, so a service
// takes the registry itself rather than a local interface naming the getters it
// calls. The service types are imported type-only: the getters name them, while
// only DefaultRegistry constructs them.

/** A token identifying a registered service (its class/constructor, a symbol, or a name). */
export type ServiceKey = unknown;

/**
 * The service lookup tokens, one per registered singleton. Java keys the lookup
 * table by `Class<?>`; a TypeScript service can be a function rather than a
 * class, so each is keyed by its name instead.
 */
export const TOKEN = {
  MessageSender: 'MessageSender',
  MessageHandler: 'MessageHandler',
  ApplicationConnection: 'ApplicationConnection',
  Heartbeat: 'Heartbeat',
  ConnectionStateHandler: 'ConnectionStateHandler',
  ServerRpcQueue: 'ServerRpcQueue',
  ApplicationConfiguration: 'ApplicationConfiguration',
  StateTree: 'StateTree',
  PushConfiguration: 'PushConfiguration',
  XhrConnection: 'XhrConnection',
  URIResolver: 'URIResolver',
  DependencyLoader: 'DependencyLoader',
  SystemErrorHandler: 'SystemErrorHandler',
  UILifecycle: 'UILifecycle',
  RequestResponseTracker: 'RequestResponseTracker',
  ReconnectConfiguration: 'ReconnectConfiguration',
  ExecuteJavaScriptProcessor: 'ExecuteJavaScriptProcessor',
  ServerConnector: 'ServerConnector',
  ResourceLoader: 'ResourceLoader',
  ConstantPool: 'ConstantPool',
  ExistingElementMap: 'ExistingElementMap',
  InitialPropertiesHandler: 'InitialPropertiesHandler',
  Poller: 'Poller',
  LoadingIndicatorStateHandler: 'LoadingIndicatorStateHandler'
} as const;

/**
 * A registry of singleton instances, such as {@link ServerRpcQueue}, which can be
 * looked up based on their class.
 */
export class Registry {
  readonly #lookupTable = new Map<ServiceKey, unknown>();

  readonly #resettable = new Map<ServiceKey, () => unknown>();

  /**
   * Stores an instance of the given type.
   *
   * @param type - the type to store
   * @param instance - the instance to store
   * @typeParam T - the type
   */
  protected set<T>(type: ServiceKey, instance: T): void {
    assert(!this.#lookupTable.has(type), 'Registry already has a class of this type registered');
    this.#lookupTable.set(type, instance);
  }

  /**
   * Stores an instance created by the given supplier and remembers the supplier
   * so the instance can be recreated by reset(). Mirrors Registry.set(Class,
   * Supplier).
   */
  protected setResettable<T>(type: ServiceKey, instanceSupplier: () => T): void {
    this.set(type, instanceSupplier());
    this.#resettable.set(type, instanceSupplier);
  }

  /**
   * Gets the instance registered for the given type. Throws if none has been
   * registered. Mirrors Registry.get(Class).
   */
  protected get<T>(type: ServiceKey): T {
    assert(this.#lookupTable.has(type), 'Tried to look up a type but no instance has been registered');
    return this.#lookupTable.get(type) as T;
  }

  /**
   * Gets the {@link MessageSender} singleton.
   *
   * @returns the {@link MessageSender} singleton
   */
  getMessageSender(): MessageSender {
    return this.get(TOKEN.MessageSender);
  }

  /**
   * Gets the {@link MessageHandler} singleton.
   *
   * @returns the {@link MessageHandler} singleton
   */
  getMessageHandler(): MessageHandler {
    return this.get(TOKEN.MessageHandler);
  }

  /**
   * Gets the {@link ApplicationConnection} singleton.
   *
   * @returns the {@link ApplicationConnection} singleton
   */
  getApplicationConnection(): ApplicationConnection {
    return this.get(TOKEN.ApplicationConnection);
  }

  /**
   * Gets the {@link Heartbeat} singleton.
   *
   * @returns the {@link Heartbeat} singleton
   */
  getHeartbeat(): Heartbeat {
    return this.get(TOKEN.Heartbeat);
  }

  /**
   * Gets the {@link ConnectionStateHandler} singleton.
   *
   * @returns the {@link ConnectionStateHandler} singleton
   */
  getConnectionStateHandler(): ConnectionStateHandler {
    return this.get(TOKEN.ConnectionStateHandler);
  }

  /**
   * Gets the {@link ServerRpcQueue} singleton.
   *
   * @returns the {@link ServerRpcQueue} singleton
   */
  getServerRpcQueue(): ServerRpcQueue {
    return this.get(TOKEN.ServerRpcQueue);
  }

  /**
   * Gets the {@link ApplicationConfiguration} singleton.
   *
   * @returns the {@link ApplicationConfiguration} singleton
   */
  getApplicationConfiguration(): ApplicationConfiguration {
    return this.get(TOKEN.ApplicationConfiguration);
  }

  /**
   * Gets the {@link StateTree} singleton.
   *
   * @returns the {@link StateTree} singleton
   */
  getStateTree(): StateTree {
    return this.get(TOKEN.StateTree);
  }

  /**
   * Gets the {@link PushConfiguration} singleton.
   *
   * @returns the {@link PushConfiguration} singleton
   */
  getPushConfiguration(): PushConfiguration {
    return this.get(TOKEN.PushConfiguration);
  }

  /**
   * Gets the {@link XhrConnection} singleton.
   *
   * @returns the {@link XhrConnection} singleton
   */
  getXhrConnection(): XhrConnection {
    return this.get(TOKEN.XhrConnection);
  }

  /**
   * Gets the {@link URIResolver} singleton.
   *
   * @returns the {@link URIResolver} singleton
   */
  getURIResolver(): URIResolver {
    return this.get(TOKEN.URIResolver);
  }

  /**
   * Gets the {@link DependencyLoader} singleton.
   *
   * @returns the {@link DependencyLoader} singleton
   */
  getDependencyLoader(): DependencyLoader {
    return this.get(TOKEN.DependencyLoader);
  }

  /**
   * Gets the {@link SystemErrorHandler} singleton.
   *
   * @returns the {@link SystemErrorHandler} singleton
   */
  getSystemErrorHandler(): SystemErrorHandler {
    return this.get(TOKEN.SystemErrorHandler);
  }

  /**
   * Gets the {@link UILifecycle} singleton.
   *
   * @returns the {@link UILifecycle} singleton
   */
  getUILifecycle(): UILifecycle {
    return this.get(TOKEN.UILifecycle);
  }

  /**
   * Gets the {@link RequestResponseTracker} singleton.
   *
   * @returns the {@link RequestResponseTracker} singleton
   */
  getRequestResponseTracker(): RequestResponseTracker {
    return this.get(TOKEN.RequestResponseTracker);
  }

  /**
   * Gets the {@link ReconnectConfiguration} singleton.
   *
   * @returns the {@link ReconnectConfiguration} singleton
   */
  getReconnectConfiguration(): ReconnectConfiguration {
    return this.get(TOKEN.ReconnectConfiguration);
  }

  /**
   * Gets the {@link ExecuteJavaScriptProcessor} singleton.
   *
   * @returns the {@link ExecuteJavaScriptProcessor} singleton
   */
  getExecuteJavaScriptProcessor(): ExecuteJavaScriptProcessor {
    return this.get(TOKEN.ExecuteJavaScriptProcessor);
  }

  /**
   * Gets the {@link ServerConnector} singleton.
   *
   * @returns the {@link ServerConnector} singleton
   */
  getServerConnector(): ServerConnector {
    return this.get(TOKEN.ServerConnector);
  }

  /**
   * Gets the {@link ResourceLoader} singleton.
   *
   * @returns the {@link ResourceLoader} singleton
   */
  getResourceLoader(): ResourceLoader {
    return this.get(TOKEN.ResourceLoader);
  }

  /**
   * Gets the {@link ConstantPool} singleton.
   *
   * @returns the {@link ConstantPool} singleton
   */
  getConstantPool(): ConstantPool {
    return this.get(TOKEN.ConstantPool);
  }

  /**
   * Gets the {@link ExistingElementMap} singleton.
   *
   * @returns the {@link ExistingElementMap} singleton
   */
  getExistingElementMap(): ExistingElementMap {
    return this.get(TOKEN.ExistingElementMap);
  }

  /**
   * Gets the {@link InitialPropertiesHandler} singleton.
   *
   * @returns the {@link InitialPropertiesHandler} singleton
   */
  getInitialPropertiesHandler(): InitialPropertiesHandler {
    return this.get(TOKEN.InitialPropertiesHandler);
  }

  /**
   * Gets the {@link Poller} singleton.
   *
   * @returns the {@link Poller} singleton
   */
  getPoller(): Poller {
    return this.get(TOKEN.Poller);
  }

  /**
   * Gets the {@link LoadingIndicatorStateHandler} singleton.
   *
   * @returns the {@link LoadingIndicatorStateHandler} singleton
   */
  getLoadingIndicatorStateHandler(): LoadingIndicatorStateHandler {
    return this.get(TOKEN.LoadingIndicatorStateHandler);
  }

  /**
   * Deletes and recreates resettable instances of registry singletons.
   */
  reset(): void {
    this.#resettable.forEach((supplier, key) => {
      this.#lookupTable.delete(key);
      this.#lookupTable.set(key, supplier());
    });
  }
}
