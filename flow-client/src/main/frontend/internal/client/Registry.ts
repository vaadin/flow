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

/**
 * Dependency-injection container for the client engine. Migrated from
 * `com.vaadin.client.Registry`. Backed by a plain {@link Map}; subclasses
 * (such as {@link DefaultRegistry}) populate the table through the
 * protected-style `set()` methods at construction time.
 *
 * The typed getters mirror the Java accessors; consumers reach back into the
 * registry through these to avoid hard imports on the concrete services.
 */
export class Registry {
  private readonly lookupTable = new Map<string, unknown>();
  private readonly resettable = new Map<string, () => unknown>();

  /**
   * Stores an instance under the given lookup key. Instances are considered
   * final unless a supplier variant of {@link set} is used.
   */
  protected set<T>(key: string, instance: T): void {
    if (this.lookupTable.has(key)) {
      throw new Error(`Registry already has a class of type ${key} registered`);
    }
    this.lookupTable.set(key, instance);
  }

  /**
   * Stores an instance under the given lookup key and registers a supplier so
   * the entry can be recreated by {@link reset}.
   */
  protected setResettable<T>(key: string, supplier: () => T): void {
    this.set(key, supplier());
    this.resettable.set(key, supplier);
  }

  /** Generic lookup. */
  protected get<T>(key: string): T {
    if (!this.lookupTable.has(key)) {
      throw new Error(`Tried to lookup type ${key} but no instance has been registered`);
    }
    return this.lookupTable.get(key) as T;
  }

  // Typed getters – mirror the Java Registry public API. The string keys are
  // chosen to match what DefaultRegistry registers; they are stable identifiers
  // rather than user-visible names.

  getMessageSender(): any {
    return this.get('MessageSender');
  }

  getMessageHandler(): any {
    return this.get('MessageHandler');
  }

  getApplicationConnection(): any {
    return this.get('ApplicationConnection');
  }

  getHeartbeat(): any {
    return this.get('Heartbeat');
  }

  getConnectionStateHandler(): any {
    return this.get('ConnectionStateHandler');
  }

  getServerRpcQueue(): any {
    return this.get('ServerRpcQueue');
  }

  getApplicationConfiguration(): any {
    return this.get('ApplicationConfiguration');
  }

  getStateTree(): any {
    return this.get('StateTree');
  }

  getPushConfiguration(): any {
    return this.get('PushConfiguration');
  }

  getXhrConnection(): any {
    return this.get('XhrConnection');
  }

  getURIResolver(): any {
    return this.get('URIResolver');
  }

  getDependencyLoader(): any {
    return this.get('DependencyLoader');
  }

  getSystemErrorHandler(): any {
    return this.get('SystemErrorHandler');
  }

  getUILifecycle(): any {
    return this.get('UILifecycle');
  }

  getRequestResponseTracker(): any {
    return this.get('RequestResponseTracker');
  }

  getReconnectConfiguration(): any {
    return this.get('ReconnectConfiguration');
  }

  getExecuteJavaScriptProcessor(): any {
    return this.get('ExecuteJavaScriptProcessor');
  }

  getServerConnector(): any {
    return this.get('ServerConnector');
  }

  getResourceLoader(): any {
    return this.get('ResourceLoader');
  }

  getConstantPool(): any {
    return this.get('ConstantPool');
  }

  getExistingElementMap(): any {
    return this.get('ExistingElementMap');
  }

  getInitialPropertiesHandler(): any {
    return this.get('InitialPropertiesHandler');
  }

  getPoller(): any {
    return this.get('Poller');
  }

  getLoadingIndicatorStateHandler(): any {
    return this.get('LoadingIndicatorStateHandler');
  }

  /** Deletes and recreates resettable instances of registry singletons. */
  reset(): void {
    this.resettable.forEach((supplier, key) => {
      this.lookupTable.delete(key);
      this.lookupTable.set(key, supplier());
    });
  }
}
