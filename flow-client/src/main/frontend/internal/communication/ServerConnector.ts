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

// TypeScript port of com.vaadin.client.communication.ServerConnector, built
// alongside the Java version. It builds RPC messages (plain JS objects, later
// JSON-serialized by the transport) and enqueues them on the server RPC queue.
// The Registry, ServerRpcQueue and LoadingIndicatorStateHandler are not ported
// yet, so the slices it needs are declared here as contracts satisfied at
// cutover; this is the real implementation of StateTree's ServerConnector
// contract.

import { encodeWithoutTypeInfo } from '../ClientJsonCodec';
import { JsonConstants } from '../JsonConstants';

/** The slice of StateNode that ServerConnector reads. */
interface ConnectorNode {
  getId(): number;
}

/** Details of an existing-element attach request; mirrors StateTree's ExistingElementAttach. */
interface ExistingElementAttach {
  requestedId: number;
  assignedId: number;
  tagName: string;
  index: number;
}

/** The slice of Registry that ServerConnector uses. */
interface ServerConnectorRegistry {
  getLoadingIndicatorStateHandler(): { processMessage(type: string | null, eventType: string | null): void };
  getServerRpcQueue(): { add(message: Record<string, unknown>): void; flush(): void };
}

/** Creates and sends messages to the server via the server RPC queue; mirrors ServerConnector.java. */
export class ServerConnector {
  private readonly registry: ServerConnectorRegistry;

  constructor(registry: ServerConnectorRegistry) {
    this.registry = registry;
  }

  /** Sends a navigation message to the server. */
  sendNavigationMessage(location: string, stateObject: unknown, routerLinkEvent: boolean): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_TYPE_NAVIGATION;
    message[JsonConstants.RPC_NAVIGATION_LOCATION] = location;
    if (stateObject !== null && stateObject !== undefined) {
      message[JsonConstants.RPC_NAVIGATION_STATE] = encodeWithoutTypeInfo(stateObject);
    }
    if (routerLinkEvent) {
      // Only the presence of the key is checked, so use a short value.
      message[JsonConstants.RPC_NAVIGATION_ROUTERLINK] = 1;
    }
    this.sendMessage(message);
  }

  /**
   * Sends an event message to the server. Accepts either a state node or its id,
   * mirroring the two `sendEventMessage` overloads in ServerConnector.java (the
   * id form is used by the published client API and connectWebComponent).
   */
  sendEventMessage(nodeOrId: ConnectorNode | number, eventType: string, eventData: unknown): void {
    const nodeId = typeof nodeOrId === 'number' ? nodeOrId : nodeOrId.getId();
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_TYPE_EVENT;
    message[JsonConstants.RPC_NODE] = nodeId;
    message[JsonConstants.RPC_EVENT_TYPE] = eventType;
    if (eventData !== null && eventData !== undefined) {
      message[JsonConstants.RPC_EVENT_DATA] = eventData;
    }
    this.sendMessage(message);
  }

  /** Sends a template (published-server-event-handler) event message to the server. */
  sendTemplateEventMessage(node: ConnectorNode, methodName: string, argsArray: unknown[], promiseId: number): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_PUBLISHED_SERVER_EVENT_HANDLER;
    message[JsonConstants.RPC_NODE] = node.getId();
    message[JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME] = methodName;
    message[JsonConstants.RPC_TEMPLATE_EVENT_ARGS] = argsArray;
    if (promiseId !== -1) {
      message[JsonConstants.RPC_TEMPLATE_EVENT_PROMISE] = promiseId;
    }
    this.sendMessage(message);
  }

  /** Sends a node map-property value sync message to the server. */
  sendNodeSyncMessage(node: ConnectorNode, feature: number, key: string, value: unknown): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_TYPE_MAP_SYNC;
    message[JsonConstants.RPC_NODE] = node.getId();
    message[JsonConstants.RPC_FEATURE] = feature;
    message[JsonConstants.RPC_PROPERTY] = key;
    message[JsonConstants.RPC_PROPERTY_VALUE] = encodeWithoutTypeInfo(value);
    this.sendMessage(message);
  }

  /** Sends an attach-existing-element callback to the server. */
  sendExistingElementAttachToServer(parent: ConnectorNode, attach: ExistingElementAttach): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_ATTACH_EXISTING_ELEMENT;
    message[JsonConstants.RPC_NODE] = parent.getId();
    message[JsonConstants.RPC_ATTACH_REQUESTED_ID] = attach.requestedId;
    message[JsonConstants.RPC_ATTACH_ASSIGNED_ID] = attach.assignedId;
    message[JsonConstants.RPC_ATTACH_TAG_NAME] = attach.tagName;
    message[JsonConstants.RPC_ATTACH_INDEX] = attach.index;
    this.sendMessage(message);
  }

  /** Sends an attach-existing-element-by-id callback to the server. */
  sendExistingElementWithIdAttachToServer(
    parent: ConnectorNode,
    requestedId: number,
    assignedId: number,
    id: string
  ): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_ATTACH_EXISTING_ELEMENT_BY_ID;
    message[JsonConstants.RPC_NODE] = parent.getId();
    message[JsonConstants.RPC_ATTACH_REQUESTED_ID] = requestedId;
    message[JsonConstants.RPC_ATTACH_ASSIGNED_ID] = assignedId;
    message[JsonConstants.RPC_ATTACH_ID] = id;
    this.sendMessage(message);
  }

  /** Sends a return-channel message to the server. */
  sendReturnChannelMessage(stateNodeId: number, channelId: number, args: unknown[]): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_TYPE_CHANNEL;
    message[JsonConstants.RPC_NODE] = stateNodeId;
    message[JsonConstants.RPC_CHANNEL] = channelId;
    message[JsonConstants.RPC_CHANNEL_ARGUMENTS] = args;
    this.sendMessage(message);
  }

  private sendMessage(message: Record<string, unknown>): void {
    this.registry
      .getLoadingIndicatorStateHandler()
      .processMessage(
        (message[JsonConstants.RPC_TYPE] as string | undefined) ?? null,
        (message[JsonConstants.RPC_EVENT_TYPE] as string | undefined) ?? null
      );
    const rpcQueue = this.registry.getServerRpcQueue();
    rpcQueue.add(message);
    rpcQueue.flush();
  }
}
