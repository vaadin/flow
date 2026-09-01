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
// earlier services through the Registry getters). This is the cutover-assembly
// step that takes the build-alongside TS engine into use.
//
// Push is wired via the AtmospherePushConnection factory, so setPushEnabled(true)
// creates a real Atmosphere connection (loading vaadinPush.js on demand).

import type { ApplicationConfiguration } from './ApplicationConfiguration';
import type { ApplicationConnection } from './ApplicationConnection';
import { atmospherePushConnectionFactory } from './communication/AtmospherePushConnection';
import { ConstantPool } from './flow/ConstantPool';
import { DependencyLoader } from './DependencyLoader';
import { DefaultConnectionStateHandler } from './communication/DefaultConnectionStateHandler';
import { Heartbeat } from './communication/Heartbeat';
import { LoadingIndicatorStateHandler } from './communication/LoadingIndicatorStateHandler';
import { MessageHandler } from './communication/MessageHandler';
import { MessageSender } from './communication/MessageSender';
import { Poller } from './communication/Poller';
import { PushConfiguration } from './communication/PushConfiguration';
import { ReconnectConfiguration } from './communication/ReconnectConfiguration';
import { Registry, TOKEN } from './Registry';
import { RequestResponseTracker } from './communication/RequestResponseTracker';
import { ResourceLoader } from './ResourceLoader';
import { ServerConnector } from './communication/ServerConnector';
import { ServerRpcQueue } from './communication/ServerRpcQueue';
import { ExecuteJavaScriptProcessor } from './flow/ExecuteJavaScriptProcessor';
import { ExistingElementMap } from './ExistingElementMap';
import { InitialPropertiesHandler } from './InitialPropertiesHandler';
import { getServerEventObjectForResync } from './flow/binding/ServerEventObject';
import { StateTree } from './flow/StateTree';
import { SystemErrorHandler } from './SystemErrorHandler';
import { UILifecycle } from './UILifecycle';
import { URIResolver } from './URIResolver';
import { XhrConnection } from './communication/XhrConnection';

/**
 * A registry implementation used by {@link ApplicationConnection}.
 */
export class DefaultRegistry extends Registry {
  /**
   * Constructs a registry based on the given configuration reference.
   *
   * Java also takes the application connection here, because the registry is
   * constructed from inside ApplicationConnection's constructor; the port
   * registers it through {@link DefaultRegistry.setApplicationConnection} instead.
   *
   * @param applicationConfiguration - the application configuration
   */
  constructor(applicationConfiguration: ApplicationConfiguration) {
    super();
    // Initialization order matters: many constructors read earlier services.
    const self = this;

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
    this.set(TOKEN.StateTree, new StateTree(self, getServerEventObjectForResync));
    this.set(TOKEN.RequestResponseTracker, new RequestResponseTracker(self));
    this.set(TOKEN.MessageHandler, new MessageHandler(self));
    this.set(TOKEN.MessageSender, new MessageSender(self, atmospherePushConnectionFactory));
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

  /**
   * Stores the application connection this registry belongs to.
   *
   * Java takes the connection as the first constructor parameter, because the
   * registry is constructed from inside ApplicationConnection's constructor. The
   * port assembles the registry first and hands the connection over as soon as it
   * exists, which is before anything can look it up.
   *
   * @param connection - the application connection
   */
  setApplicationConnection(connection: ApplicationConnection): void {
    this.set(TOKEN.ApplicationConnection, connection);
  }
}
