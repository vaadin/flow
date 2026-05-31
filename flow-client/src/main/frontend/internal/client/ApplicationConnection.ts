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
import { LoadingIndicatorConfigurator } from './communication/LoadingIndicatorConfigurator';
import { PollConfigurator } from './communication/PollConfigurator';
import { Binder } from './flow/binding/Binder';
import { Computation } from './flow/reactive/Computation';
import type { StateNode } from './flow/StateNode';
import { DefaultRegistry } from './DefaultRegistry';
import type { Registry } from './Registry';

// Mirrors com.vaadin.flow.internal.nodefeature.NodeFeatures.
const NF_ELEMENT_DATA = 0;
const NF_ELEMENT_STYLE_PROPERTIES = 12;

// Mirrors com.vaadin.flow.internal.nodefeature.NodeProperties.
const PROP_VISIBLE = 'visible';
const PROP_JAVA_CLASS = 'class';

interface FlowClientsContainer {
  clients?: Record<string, unknown>;
}

interface FlowGlobal {
  Vaadin?: { Flow?: FlowClientsContainer };
}

function flowClients(): Record<string, unknown> | undefined {
  const flow = (globalThis as unknown as FlowGlobal).Vaadin?.Flow;
  if (!flow) {
    return undefined;
  }
  flow.clients = flow.clients ?? {};
  return flow.clients;
}

/**
 * Main class for an application / UI. Initializes the registry, binds the
 * root state node to the DOM body and publishes the per-application JS API
 * under `window.Vaadin.Flow.clients[appId]`. Migrated from
 * `com.vaadin.client.ApplicationConnection`.
 */
export class ApplicationConnection {
  private readonly registry: Registry;

  constructor(applicationConfiguration: ApplicationConfiguration) {
    this.registry = new DefaultRegistry(this, applicationConfiguration);

    // Surface uncaught errors from async callbacks through the system error
    // handler. GWT used `GWT.setUncaughtExceptionHandler`; in the browser the
    // closest equivalent is the global `error` event.
    const systemErrorHandler = this.registry.getSystemErrorHandler();
    window.addEventListener('error', (event: ErrorEvent) => {
      systemErrorHandler.handleError(event.error?.message ?? event.message ?? String(event.error));
    });

    const rootNode = this.registry.getStateTree().getRootNode() as StateNode;

    // Bind UI configuration objects
    PollConfigurator.observe(rootNode as unknown as never, this.registry.getPoller());
    bindReconnectConfiguration(this.registry.getConnectionStateHandler());
    LoadingIndicatorConfigurator.observe(rootNode as unknown as never);

    const body = document.body;

    rootNode.setDomNode(body);
    Binder.bind(rootNode, body);

    const appId = applicationConfiguration.applicationId as string;
    Console.debug(`Starting application ${appId}`);

    // Remove the auto-generated window-name suffix (`-123`) before publishing
    // the per-application JS API; the client-facing testbench id is the bare
    // root panel name.
    const appRootPanelName = appId.replace(/-\d+$/u, '');

    const productionMode = applicationConfiguration.productionMode;
    const requestTiming = applicationConfiguration.requestTiming;
    this.publishJavascriptMethods(
      appRootPanelName,
      productionMode,
      requestTiming,
      applicationConfiguration.exportedWebComponents ?? []
    );
    if (!productionMode) {
      const servletVersion = applicationConfiguration.servletVersion ?? '';
      this.publishDevelopmentModeJavascriptMethods(appRootPanelName, servletVersion);
      Console.debug(`Vaadin application servlet version: ${servletVersion}`);
    }
  }

  /**
   * Starts this application. Called by the bootstrapper which ensures
   * applications are started in order.
   */
  start(initialUidl: unknown): void {
    if (initialUidl == null) {
      // initial UIDL not in DOM, request from server
      this.registry.getMessageSender().resynchronize();
    } else {
      // initial UIDL provided in DOM, continue as if returned by request

      // Hack to avoid logging an error in endRequest()
      this.registry.getRequestResponseTracker().startRequest();
      this.registry.getMessageHandler().handleMessage(initialUidl);
    }

    window.addEventListener('pagehide', () => {
      this.registry.getMessageSender().sendUnloadBeacon();
    });

    window.addEventListener('pageshow', () => {
      // Currently only Safari gets here, sometimes when going back/forward
      // with browser buttons. Chrome discards our state as beforeunload is
      // used. As state is most likely cleared on the server already
      // (especially now with Beacon API request), it is probably better
      // resynchronize the state (would happen on first server visit).
      window.location.reload();
    });
  }

  /** Returns true if the client has some work to be done. */
  private isActive(): boolean {
    return (
      !this.registry.getMessageHandler().isInitialUidlHandled() ||
      this.registry.getRequestResponseTracker().hasActiveRequest()
    );
  }

  private getDomElementByNodeId(id: number): unknown {
    const node = this.registry.getStateTree().getNode(id);
    return node == null ? null : node.getDomNode();
  }

  private getJavaClass(id: number): string | null {
    const node = this.registry.getStateTree().getNode(id);
    if (node == null) {
      return null;
    }
    return node.getMap(NF_ELEMENT_DATA).getProperty(PROP_JAVA_CLASS).getValueOrDefault(null);
  }

  private isHiddenByServer(id: number): boolean {
    const node = this.registry.getStateTree().getNode(id);
    const visible =
      node == null ? true : node.getMap(NF_ELEMENT_DATA).getProperty(PROP_VISIBLE).getValueOrDefault(true);
    return !visible;
  }

  private getElementStyleProperties(id: number): Record<string, unknown> {
    const node = this.registry.getStateTree().getNode(id);
    const styles: Record<string, unknown> = {};
    if (node != null) {
      const styleMap = node.getMap(NF_ELEMENT_STYLE_PROPERTIES);
      const names = styleMap.getPropertyNames();
      for (let i = 0; i < names.length; i++) {
        const name = names.get(i) as string;
        styles[name] = styleMap.getProperty(name).getValue();
      }
    }
    return styles;
  }

  private getNodeId(element: unknown): number {
    const node = this.registry.getStateTree().getStateNodeForDomNode(element);
    return node == null ? -1 : node.getId();
  }

  private addDomSetListener(nodeId: number, callback: () => void): void {
    const stateNode = this.registry.getStateTree().getNode(nodeId);
    if (stateNode == null) {
      return;
    }
    stateNode.addDomNodeSetListener((node: { getId(): number }) => {
      if (nodeId === node.getId()) {
        callback();
        return true;
      }
      return false;
    });
  }

  /** Per-application API published under `window.Vaadin.Flow.clients[appId]`. */

  private publishJavascriptMethods(
    applicationId: string,
    productionMode: boolean,
    requestTiming: boolean,
    exportedWebComponents: string[]
  ): void {
    const clients = flowClients();
    if (!clients) {
      return;
    }
    const rootNodeIdProvider = () => this.registry.getStateTree().getRootNode().getId();
    const client: Record<string, unknown> = {
      isActive: () => this.isActive(),
      getByNodeId: (nodeId: number) => this.getDomElementByNodeId(nodeId),
      getNodeId: (element: unknown) => this.getNodeId(element),
      getUIId: () => this.registry.getApplicationConfiguration().uiId,
      addDomBindingListener: (nodeId: number, callback: () => void) => this.addDomSetListener(nodeId, callback),
      productionMode,
      poll: () => this.registry.getPoller().poll(),
      connectWebComponent: (eventData: unknown) =>
        this.registry
          .getServerConnector()
          .sendEventMessageByNodeId(rootNodeIdProvider(), 'connect-web-component', eventData),
      resolveUri: (uri: string) => this.registry.getURIResolver().resolveVaadinUri(uri),
      sendEventMessage: (nodeId: number, eventType: string, eventData: unknown) =>
        this.registry.getServerConnector().sendEventMessageByNodeId(nodeId, eventType, eventData),
      initializing: false,
      exportedWebComponents
    };
    if (requestTiming) {
      client.getProfilingData = () => this.registry.getMessageHandler().getProfilingData();
    }
    clients[applicationId] = client;
  }

  /** Adds development-only methods to the per-application API. */
  private publishDevelopmentModeJavascriptMethods(applicationId: string, servletVersion: string): void {
    const client = flowClients()?.[applicationId] as Record<string, unknown> | undefined;
    if (!client) {
      return;
    }
    client.isActive = () => this.isActive();
    client.getVersionInfo = () => ({ flow: servletVersion });
    client.debug = () => this.registry.getStateTree().getRootNode().getDebugJson();
    client.getNodeInfo = (nodeId: number) => ({
      element: this.getDomElementByNodeId(nodeId),
      javaClass: this.getJavaClass(nodeId),
      hiddenByServer: this.isHiddenByServer(nodeId),
      styles: this.getElementStyleProperties(nodeId)
    });
  }
}

/**
 * Reactive subscription: re-runs `configurationUpdated()` whenever the
 * reconnect-dialog configuration on the root node changes.
 */
function bindReconnectConfiguration(stateHandler: { configurationUpdated(): void }): void {
  // Constructing a Computation registers the callback and primes the initial
  // dependency snapshot. We intentionally hold no reference to the result;
  // the computation lives as long as its dependencies do.
  new Computation(() => stateHandler.configurationUpdated());
}
