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

// TypeScript port of com.vaadin.client.DefaultRegistry — the concrete service
// assembly used by ApplicationConnection. It instantiates the ported TS services
// in dependency order (initialization order matters: later constructors read
// earlier services via the getters) and exposes the typed getters every service
// resolves its collaborators through. This is the cutover-assembly step that
// takes the build-alongside TS engine into use.
//
// Push is wired XHR-only for now: MessageSender is created without a
// PushConnectionFactory, so setPushEnabled(true) is a no-op until the full
// AtmospherePushConnection class is wired in as the factory.

import type { ApplicationConfiguration } from './ApplicationConfiguration';
import { ConstantPool } from './ConstantPool';
import { DependencyLoader } from './DependencyLoader';
import { DefaultConnectionStateHandler } from './communication/DefaultConnectionStateHandler';
import { Heartbeat } from './communication/Heartbeat';
import { LoadingIndicatorStateHandler } from './communication/LoadingIndicatorStateHandler';
import { MessageHandler } from './MessageHandler';
import { MessageSender } from './communication/MessageSender';
import { Poller } from './communication/Poller';
import { PushConfiguration } from './communication/PushConfiguration';
import { ReconnectConfiguration } from './communication/ReconnectConfiguration';
import { Registry } from './Registry';
import { RequestResponseTracker } from './communication/RequestResponseTracker';
import { ResourceLoader } from './ResourceLoader';
import { ServerConnector } from './communication/ServerConnector';
import { ServerRpcQueue } from './communication/ServerRpcQueue';
import { ExecuteJavaScriptProcessor } from './ExecuteJavaScriptProcessor';
import { ExistingElementMap } from './ExistingElementMap';
import { InitialPropertiesHandler } from './InitialPropertiesHandler';
import { StateTree } from './StateTree';
import { SystemErrorHandler } from './SystemErrorHandler';
import { UILifecycle } from './UILifecycle';
import { URIResolver } from './URIResolver';
import { XhrConnection } from './XhrConnection';

// Service lookup tokens (one per registered singleton).
const TOKEN = {
  ApplicationConfiguration: 'ApplicationConfiguration',
  ResourceLoader: 'ResourceLoader',
  URIResolver: 'URIResolver',
  DependencyLoader: 'DependencyLoader',
  SystemErrorHandler: 'SystemErrorHandler',
  UILifecycle: 'UILifecycle',
  StateTree: 'StateTree',
  RequestResponseTracker: 'RequestResponseTracker',
  MessageHandler: 'MessageHandler',
  MessageSender: 'MessageSender',
  ServerRpcQueue: 'ServerRpcQueue',
  ServerConnector: 'ServerConnector',
  ExecuteJavaScriptProcessor: 'ExecuteJavaScriptProcessor',
  ConstantPool: 'ConstantPool',
  ExistingElementMap: 'ExistingElementMap',
  InitialPropertiesHandler: 'InitialPropertiesHandler',
  Heartbeat: 'Heartbeat',
  ConnectionStateHandler: 'ConnectionStateHandler',
  XhrConnection: 'XhrConnection',
  PushConfiguration: 'PushConfiguration',
  ReconnectConfiguration: 'ReconnectConfiguration',
  Poller: 'Poller',
  LoadingIndicatorStateHandler: 'LoadingIndicatorStateHandler'
} as const;

/** The concrete registry that wires the TS services; mirrors DefaultRegistry.java. */
export class DefaultRegistry extends Registry {
  constructor(applicationConfiguration: ApplicationConfiguration) {
    super();
    // Initialization order matters: many constructors read earlier services.
    const self = this as never;

    this.set(TOKEN.ApplicationConfiguration, applicationConfiguration);

    // No constructor dependencies (resolve collaborators lazily via getters).
    this.set(
      TOKEN.ResourceLoader,
      // ResourceLoader takes an error handler directly; adapt it to resolve the
      // SystemErrorHandler lazily (it is registered just below).
      new ResourceLoader({ handleError: (message: string) => this.getSystemErrorHandler().handleError(message) }, true)
    );
    this.set(TOKEN.URIResolver, new URIResolver(self));
    this.set(TOKEN.DependencyLoader, new DependencyLoader(self));
    this.set(TOKEN.SystemErrorHandler, new SystemErrorHandler(self));
    this.setResettable(TOKEN.UILifecycle, () => new UILifecycle());
    this.set(TOKEN.StateTree, new StateTree(self));
    this.set(TOKEN.RequestResponseTracker, new RequestResponseTracker(self));
    this.set(TOKEN.MessageHandler, new MessageHandler(self));
    this.set(TOKEN.MessageSender, new MessageSender(self));
    this.set(TOKEN.ServerRpcQueue, new ServerRpcQueue(self));
    this.set(TOKEN.ServerConnector, new ServerConnector(self));
    this.set(TOKEN.ExecuteJavaScriptProcessor, new ExecuteJavaScriptProcessor(self));
    this.setResettable(TOKEN.ConstantPool, () => new ConstantPool());
    this.setResettable(TOKEN.ExistingElementMap, () => new ExistingElementMap());
    this.set(TOKEN.InitialPropertiesHandler, new InitialPropertiesHandler(self));

    // Classes with dependencies, in order.
    this.setResettable(TOKEN.Heartbeat, () => new Heartbeat(self));
    this.set(TOKEN.ConnectionStateHandler, new DefaultConnectionStateHandler(self));
    this.set(TOKEN.XhrConnection, new XhrConnection(self));
    this.set(TOKEN.PushConfiguration, new PushConfiguration(self));
    this.set(TOKEN.ReconnectConfiguration, new ReconnectConfiguration(self));
    this.set(TOKEN.Poller, new Poller(self));
    this.set(TOKEN.LoadingIndicatorStateHandler, new LoadingIndicatorStateHandler(self));
  }

  getApplicationConfiguration(): ApplicationConfiguration {
    return this.get(TOKEN.ApplicationConfiguration);
  }

  getResourceLoader(): ResourceLoader {
    return this.get(TOKEN.ResourceLoader);
  }

  getURIResolver(): URIResolver {
    return this.get(TOKEN.URIResolver);
  }

  getDependencyLoader(): DependencyLoader {
    return this.get(TOKEN.DependencyLoader);
  }

  getSystemErrorHandler(): SystemErrorHandler {
    return this.get(TOKEN.SystemErrorHandler);
  }

  getUILifecycle(): UILifecycle {
    return this.get(TOKEN.UILifecycle);
  }

  getStateTree(): StateTree {
    return this.get(TOKEN.StateTree);
  }

  getRequestResponseTracker(): RequestResponseTracker {
    return this.get(TOKEN.RequestResponseTracker);
  }

  getMessageHandler(): MessageHandler {
    return this.get(TOKEN.MessageHandler);
  }

  getMessageSender(): MessageSender {
    return this.get(TOKEN.MessageSender);
  }

  getServerRpcQueue(): ServerRpcQueue {
    return this.get(TOKEN.ServerRpcQueue);
  }

  getServerConnector(): ServerConnector {
    return this.get(TOKEN.ServerConnector);
  }

  getExecuteJavaScriptProcessor(): ExecuteJavaScriptProcessor {
    return this.get(TOKEN.ExecuteJavaScriptProcessor);
  }

  getConstantPool(): ConstantPool {
    return this.get(TOKEN.ConstantPool);
  }

  getExistingElementMap(): ExistingElementMap {
    return this.get(TOKEN.ExistingElementMap);
  }

  getInitialPropertiesHandler(): InitialPropertiesHandler {
    return this.get(TOKEN.InitialPropertiesHandler);
  }

  getHeartbeat(): Heartbeat {
    return this.get(TOKEN.Heartbeat);
  }

  getConnectionStateHandler(): DefaultConnectionStateHandler {
    return this.get(TOKEN.ConnectionStateHandler);
  }

  getXhrConnection(): XhrConnection {
    return this.get(TOKEN.XhrConnection);
  }

  getPushConfiguration(): PushConfiguration {
    return this.get(TOKEN.PushConfiguration);
  }

  getReconnectConfiguration(): ReconnectConfiguration {
    return this.get(TOKEN.ReconnectConfiguration);
  }

  getPoller(): Poller {
    return this.get(TOKEN.Poller);
  }

  getLoadingIndicatorStateHandler(): LoadingIndicatorStateHandler {
    return this.get(TOKEN.LoadingIndicatorStateHandler);
  }
}
