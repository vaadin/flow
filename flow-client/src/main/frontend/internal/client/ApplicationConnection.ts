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
 * Builds the per-application `window.Vaadin.Flow.clients[appId]` API object.
 * Migrated from the two JSNI bodies in
 * `com.vaadin.client.ApplicationConnection`. The Java shim passes each
 * Java method as an individual positional callback so the TS side never
 * has to call back into Java by name.
 */
export const ApplicationConnection = {
  // eslint-disable-next-line @typescript-eslint/max-params
  publishJavascriptMethods(
    applicationId: string,
    productionMode: boolean,
    requestTiming: boolean,
    exportedWebComponents: string[],
    isActive: () => boolean,
    getByNodeId: (nodeId: number) => unknown,
    getNodeId: (element: unknown) => number,
    getUIId: () => number,
    addDomBindingListener: (nodeId: number, callback: unknown) => void,
    poll: () => void,
    connectWebComponent: (eventData: unknown) => void,
    resolveUri: (uri: string) => string,
    sendEventMessage: (nodeId: number, eventType: string, eventData: unknown) => void,
    getProfilingData: (() => unknown[]) | null
  ): void {
    const clients = flowClients();
    if (!clients) {
      return;
    }
    const client: Record<string, unknown> = {
      isActive: () => isActive(),
      getByNodeId: (nodeId: number) => getByNodeId(nodeId),
      getNodeId: (element: unknown) => getNodeId(element),
      getUIId: () => getUIId(),
      addDomBindingListener: (nodeId: number, callback: unknown) => addDomBindingListener(nodeId, callback),
      productionMode,
      poll: () => poll(),
      connectWebComponent: (eventData: unknown) => connectWebComponent(eventData),
      resolveUri: (uri: string) => resolveUri(uri),
      sendEventMessage: (nodeId: number, eventType: string, eventData: unknown) =>
        sendEventMessage(nodeId, eventType, eventData),
      initializing: false,
      exportedWebComponents
    };
    if (requestTiming && getProfilingData) {
      client.getProfilingData = () => getProfilingData();
    }
    clients[applicationId] = client;
  },

  // eslint-disable-next-line @typescript-eslint/max-params
  publishDevelopmentModeJavascriptMethods(
    applicationId: string,
    servletVersion: string,
    isActive: () => boolean,
    getDebugJson: () => unknown,
    getDomElementByNodeId: (nodeId: number) => unknown,
    getJavaClass: (nodeId: number) => string,
    isHiddenByServer: (nodeId: number) => boolean,
    getElementStyleProperties: (nodeId: number) => unknown
  ): void {
    const client = flowClients()?.[applicationId] as Record<string, unknown> | undefined;
    if (!client) {
      return;
    }
    client.isActive = () => isActive();
    client.getVersionInfo = () => ({ flow: servletVersion });
    client.debug = () => getDebugJson();
    client.getNodeInfo = (nodeId: number) => ({
      element: getDomElementByNodeId(nodeId),
      javaClass: getJavaClass(nodeId),
      hiddenByServer: isHiddenByServer(nodeId),
      styles: getElementStyleProperties(nodeId)
    });
  }
};
