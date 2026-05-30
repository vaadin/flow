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

import type { ApplicationConfiguration } from './ApplicationConfiguration';
import { Console } from './Console';
import { DependencyLoader } from './DependencyLoader';
import { ExistingElementMap } from './ExistingElementMap';
import { InitialPropertiesHandler } from './InitialPropertiesHandler';
import { Registry } from './Registry';
import { ResourceLoader } from './ResourceLoader';
import { SystemErrorHandler, type SystemErrorHandlerCallbacks } from './SystemErrorHandler';
import { UILifecycle } from './UILifecycle';
import { URIResolver } from './URIResolver';
import { WidgetUtil } from './WidgetUtil';
import {
  AtmospherePushConnection,
  type AtmospherePushConnectionCallbacks
} from './communication/AtmospherePushConnection';
import {
  DefaultConnectionStateHandler,
  type DefaultConnectionStateHandlerCallbacks
} from './communication/DefaultConnectionStateHandler';
import { Heartbeat } from './communication/Heartbeat';
import { LoadingIndicatorStateHandler } from './communication/LoadingIndicatorStateHandler';
import { MessageHandler, type MessageHandlerCallbacks } from './communication/MessageHandler';
import { MessageSender, type MessageSenderCallbacks } from './communication/MessageSender';
import { Poller } from './communication/Poller';
import { PushConfiguration } from './communication/PushConfiguration';
import { ReconnectConfiguration } from './communication/ReconnectConfiguration';
import { RequestResponseTracker } from './communication/RequestResponseTracker';
import { ServerConnector } from './communication/ServerConnector';
import { ServerRpcQueue } from './communication/ServerRpcQueue';
import { XhrConnection, type XhrConnectionCallbacks } from './communication/XhrConnection';
import { ConstantPool } from './flow/ConstantPool';
import { ExecuteJavaScriptProcessor } from './flow/ExecuteJavaScriptProcessor';
import { StateTree } from './flow/StateTree';

// Mirrors ApplicationConstants.REQUEST_TYPE_PARAMETER /
// REQUEST_TYPE_HEARTBEAT / UI_ID_PARAMETER / UI_ID. Kept inline so this module
// doesn't need a flow-shared bridge.
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_HEARTBEAT = 'heartbeat';
const UI_ID_PARAMETER = 'v-uiId';
const UI_ID = 'v-uiId';

/** Mirrors com.vaadin.flow.shared.util.SharedUtil.addGetParameter. */
function addGetParameter(uri: string, parameter: string, value: string | number): string {
  const separator = uri.includes('?') ? '&' : '?';
  return `${uri}${separator}${parameter}=${value}`;
}

/**
 * Concrete {@link Registry} populated with every service the client engine
 * needs. Migrated from `com.vaadin.client.DefaultRegistry`. Construction order
 * follows the Java version because many services capture each other by
 * reference (or by lazy `this::getX` callbacks).
 */
export class DefaultRegistry extends Registry {
  constructor(connection: unknown, applicationConfiguration: ApplicationConfiguration) {
    super();

    // Note that initialization order matters. Many constructors depend on
    // ApplicationConnection, ApplicationConfiguration and StateTree even
    // though this is not explicitly specified anywhere.

    this.set('ApplicationConnection', connection);
    this.set('ApplicationConfiguration', applicationConfiguration);

    // Classes with no constructor dependencies
    const resourceLoader = new ResourceLoader((message) => this.getSystemErrorHandler().handleError(message), true);
    this.set('ResourceLoader', resourceLoader);
    const uriResolver = new URIResolver(this);
    this.set('URIResolver', uriResolver);
    this.set(
      'DependencyLoader',
      new DependencyLoader({ resolveVaadinUri: (uri) => uriResolver.resolveVaadinUri(uri) ?? uri }, resourceLoader)
    );
    this.setResettable('UILifecycle', () => new UILifecycle());
    const uiLifecycle = this.getUILifecycle();
    const stateTree = new StateTree(this);
    this.set('StateTree', stateTree);
    const requestResponseTracker = new RequestResponseTracker(() => {
      const ms = this.getMessageSender();
      if (
        (this.getUILifecycle().isRunning() && this.getServerRpcQueue().isFlushPending()) ||
        ms.getResynchronizationState() === 'SEND_TO_SERVER' ||
        ms.hasQueuedMessages()
      ) {
        ms.sendInvocationsToServer();
      }
    });
    this.set('RequestResponseTracker', requestResponseTracker);

    const messageHandlerCallbacks: MessageHandlerCallbacks = {
      getMessageSender: () => this.getMessageSender(),
      getUiLifecycle: () => this.getUILifecycle(),
      getStateTree: () => this.getStateTree(),
      getConstantPool: () => this.getConstantPool(),
      getSystemErrorHandler: () => this.getSystemErrorHandler(),
      getExecuteJavaScriptProcessor: () => this.getExecuteJavaScriptProcessor(),
      getDependencyLoader: () => this.getDependencyLoader(),
      getResourceLoader: () => this.getResourceLoader(),
      getRequestResponseTracker: () => this.getRequestResponseTracker(),
      getLoadingIndicatorStateHandler: () => this.getLoadingIndicatorStateHandler(),
      getApplicationConfiguration: () => this.getApplicationConfiguration(),
      redirect: (url: string) => WidgetUtil.redirect(url)
    };
    this.set('MessageHandler', new MessageHandler(messageHandlerCallbacks));

    const messageSenderCallbacks: MessageSenderCallbacks = {
      getMessageHandler: () => this.getMessageHandler(),
      getUiLifecycle: () => this.getUILifecycle(),
      getRequestResponseTracker: () => this.getRequestResponseTracker(),
      getLoadingIndicatorStateHandler: () => this.getLoadingIndicatorStateHandler(),
      getPushConfiguration: () => this.getPushConfiguration(),
      getServerRpcQueue: () => this.getServerRpcQueue(),
      getApplicationConfiguration: () => this.getApplicationConfiguration(),
      sendXhr: (payload) => this.getXhrConnection().send(payload),
      getXhrUri: () => this.getXhrConnection().getUri(),
      createPushConnection: () => this.createPushConnection()
    };
    const messageSender = new MessageSender(messageSenderCallbacks);
    this.set('MessageSender', messageSender);

    const serverRpcQueue = new ServerRpcQueue(uiLifecycle, () => messageSender.sendInvocationsToServer());
    this.set('ServerRpcQueue', serverRpcQueue);
    const loadingIndicatorStateHandler = new LoadingIndicatorStateHandler(() =>
      requestResponseTracker.hasActiveRequest()
    );
    this.set('LoadingIndicatorStateHandler', loadingIndicatorStateHandler);
    const serverConnector = new ServerConnector(loadingIndicatorStateHandler, serverRpcQueue);
    this.set('ServerConnector', serverConnector);
    stateTree.setServerConnector(serverConnector);
    this.set('ExecuteJavaScriptProcessor', new ExecuteJavaScriptProcessor(this));
    this.setResettable('ConstantPool', () => new ConstantPool());
    this.setResettable('ExistingElementMap', () => new ExistingElementMap());
    const initialPropertiesHandler = new InitialPropertiesHandler(stateTree as never);
    this.set('InitialPropertiesHandler', initialPropertiesHandler);
    stateTree.setInitialPropertiesHandler(initialPropertiesHandler);

    // Classes with dependencies, in correct order
    this.setResettable('Heartbeat', () => {
      let uri = addGetParameter(
        applicationConfiguration.serviceUrl ?? '',
        REQUEST_TYPE_PARAMETER,
        REQUEST_TYPE_HEARTBEAT
      );
      uri = addGetParameter(uri, UI_ID_PARAMETER, applicationConfiguration.uiId);
      const callbacks = {
        onOk: () => this.getConnectionStateHandler().heartbeatOk(),
        onInvalidStatusCode: (xhr: XMLHttpRequest) => this.getConnectionStateHandler().heartbeatInvalidStatusCode(xhr),
        onException: (xhr: XMLHttpRequest, message: string) =>
          this.getConnectionStateHandler().heartbeatException(xhr, message)
      };
      return new Heartbeat(uri, applicationConfiguration.heartbeatInterval, uiLifecycle, callbacks);
    });

    const connectionStateCallbacks: DefaultConnectionStateHandlerCallbacks = {
      getUiLifecycle: () => this.getUILifecycle(),
      getSystemErrorHandler: () => this.getSystemErrorHandler(),
      getHeartbeat: () => this.getHeartbeat(),
      getReconnectConfiguration: () => this.getReconnectConfiguration(),
      getRequestResponseTracker: () => this.getRequestResponseTracker(),
      getLoadingIndicatorStateHandler: () => this.getLoadingIndicatorStateHandler(),
      getApplicationConfiguration: () => this.getApplicationConfiguration(),
      getMessageSender: () => this.getMessageSender()
    };
    this.set('ConnectionStateHandler', new DefaultConnectionStateHandler(connectionStateCallbacks));
    const xhrConnectionCallbacks: XhrConnectionCallbacks = {
      getMessageHandler: () => this.getMessageHandler(),
      getConnectionStateHandler: () => this.getConnectionStateHandler(),
      getRequestResponseTracker: () => this.getRequestResponseTracker(),
      getApplicationConfiguration: () => this.getApplicationConfiguration()
    };
    this.set('XhrConnection', new XhrConnection(xhrConnectionCallbacks));
    this.set(
      'PushConfiguration',
      new PushConfiguration(
        stateTree,
        () => messageSender.setPushEnabled(true),
        () => messageSender.setPushEnabled(false)
      )
    );
    this.set('ReconnectConfiguration', new ReconnectConfiguration(stateTree));
    this.set('Poller', new Poller(stateTree, uiLifecycle));

    // Wire SystemErrorHandler last so its callbacks can reach the rest of
    // the registry through `this.get*()` lookups without forward references.
    const systemErrorCallbacks: SystemErrorHandlerCallbacks = {
      getServiceUrl: () => applicationConfiguration.serviceUrl ?? '',
      isWebComponentMode: () => applicationConfiguration.webComponentMode,
      isProductionMode: () => applicationConfiguration.productionMode,
      getSessionExpiredError: () => applicationConfiguration.sessionExpiredError as never,
      getExportedWebComponents: () => applicationConfiguration.exportedWebComponents ?? [],
      getHeartbeatInterval: () => applicationConfiguration.heartbeatInterval,
      setHeartbeatInterval: (seconds: number) => this.getHeartbeat().setInterval(seconds),
      isPushEnabled: () => this.getPushConfiguration().isPushEnabled(),
      setPushEnabled: (enabled: boolean) => this.getMessageSender().setPushEnabled(enabled),
      disablePushImmediately: () => this.getMessageSender().setPushEnabled(false, false),
      applyResyncResponse: (responseText: string) => {
        const uiId = applicationConfiguration.uiId;
        const json = MessageHandler.parseJson(responseText);
        const newUiId = (json?.[UI_ID] as number) ?? uiId;
        if (newUiId !== uiId) {
          Console.debug(`UI ID switched from ${uiId} to ${newUiId} after resynchronization`);
          applicationConfiguration.uiId = newUiId;
        }
        this.reset();
        this.getUILifecycle().setStateName('RUNNING');
        this.getMessageHandler().handleMessage(json);
      }
    };
    this.set('SystemErrorHandler', new SystemErrorHandler(systemErrorCallbacks));
  }

  /**
   * Builds the callbacks struct for a new {@link AtmospherePushConnection} and
   * returns the freshly constructed instance. Used by {@link MessageSender}
   * (through the {@code createPushConnection} callback) so the push connection
   * is only instantiated when push is actually enabled.
   */
  private createPushConnection(): AtmospherePushConnection {
    const pushCallbacks: AtmospherePushConnectionCallbacks = {
      getUiLifecycle: () => this.getUILifecycle(),
      getPushConfiguration: () => this.getPushConfiguration(),
      getApplicationConfiguration: () => this.getApplicationConfiguration(),
      getURIResolver: () => this.getURIResolver(),
      getMessageHandler: () => this.getMessageHandler(),
      getConnectionStateHandler: () => this.getConnectionStateHandler(),
      getResourceLoader: () => this.getResourceLoader()
    };
    return new AtmospherePushConnection(pushCallbacks);
  }
}
