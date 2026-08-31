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
// ServerRpcQueue and LoadingIndicatorStateHandler are ported alongside this
// module; the registry slice below still names only what this class calls,
// because Registry.java's typed getters cannot be ported until DefaultRegistry
// can assemble them. This is the real implementation of StateTree's
// ServerConnector contract.

import type { LoadingIndicatorStateHandler } from './LoadingIndicatorStateHandler';
import type { ServerRpcQueue } from './ServerRpcQueue';
import type { StateNode } from '../flow/StateNode';
import { encodeWithoutTypeInfo } from '../flow/util/ClientJsonCodec';
import { JsonConstants } from '../../flow/shared/JsonConstants';

/** The slice of Registry that ServerConnector uses. */
interface ServerConnectorRegistry {
  getLoadingIndicatorStateHandler(): Pick<LoadingIndicatorStateHandler, 'processMessage'>;
  getServerRpcQueue(): Pick<ServerRpcQueue, 'add' | 'flush'>;
}

/**
 * Creates and sends messages to the server via the server RPC queue; mirrors
 * ServerConnector.java. StateTree's registry slice names the
 * subset of this class it calls with `Pick<…>`, so no contract duplicates these
 * signatures.
 */
export class ServerConnector {
  readonly #registry: ServerConnectorRegistry;

  /**
   * Creates a new server connector.
   *
   * @param registry - the global registry
   */
  constructor(registry: ServerConnectorRegistry) {
    this.#registry = registry;
  }

  /**
   * Sends a navigation message to server.
   *
   * @param location - the relative location of the navigation
   * @param stateObject - the state object or `null` if none applicable
   * @param routerLinkEvent - `true` if this event was triggered by interaction
   *          with a router link; `false` if triggered by history navigation
   */
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
    this.#sendMessage(message);
  }

  /**
   * Sends an event message to the server.
   *
   * @param nodeOrId - the node that listened to the event
   * @param eventType - the type of event
   * @param eventData - extra data associated with the event
   */
  sendEventMessage(nodeOrId: StateNode | number, eventType: string, eventData: unknown): void {
    const nodeId = typeof nodeOrId === 'number' ? nodeOrId : nodeOrId.getId();
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_TYPE_EVENT;
    message[JsonConstants.RPC_NODE] = nodeId;
    message[JsonConstants.RPC_EVENT_TYPE] = eventType;
    if (eventData !== null && eventData !== undefined) {
      message[JsonConstants.RPC_EVENT_DATA] = eventData;
    }
    this.#sendMessage(message);
  }

  /**
   * Sends a template event message to the server.
   *
   * @param node - the node that listened to the event
   * @param methodName - the event handler method name to execute on the server side
   * @param argsArray - the arguments array for the method
   * @param promiseId - the promise id to use for getting the result back, or -1 if no
   *          * result is expected
   */
  sendTemplateEventMessage(node: StateNode, methodName: string, argsArray: unknown[], promiseId: number): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_PUBLISHED_SERVER_EVENT_HANDLER;
    message[JsonConstants.RPC_NODE] = node.getId();
    message[JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME] = methodName;
    message[JsonConstants.RPC_TEMPLATE_EVENT_ARGS] = argsArray;
    if (promiseId !== -1) {
      message[JsonConstants.RPC_TEMPLATE_EVENT_PROMISE] = promiseId;
    }
    this.#sendMessage(message);
  }

  /**
   * Sends a node value sync message to the server.
   *
   * @param node - the node to update
   * @param feature - the id of the node map feature to update
   * @param key - the map key to update
   * @param value - the new value
   */
  sendNodeSyncMessage(node: StateNode, feature: number, key: string, value: unknown): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_TYPE_MAP_SYNC;
    message[JsonConstants.RPC_NODE] = node.getId();
    message[JsonConstants.RPC_FEATURE] = feature;
    message[JsonConstants.RPC_PROPERTY] = key;
    message[JsonConstants.RPC_PROPERTY_VALUE] = encodeWithoutTypeInfo(value);
    this.#sendMessage(message);
  }

  /** Sends an attach-existing-element callback to the server. */

  /**
   * Sends a data for attach existing element server side callback.
   *
   * @param parent - parent of the node to attach
   * @param requestedId - originally requested id of a server side node
   * @param assignedId - identifier which should be used on the server side for the
   *          element (instead of requestedId)
   * @param tagName - the requested tagName
   * @param index - the index of the element on the server side
   */
  // eslint-disable-next-line @typescript-eslint/max-params -- mirrors the Java signature
  sendExistingElementAttachToServer(
    parent: StateNode,
    requestedId: number,
    assignedId: number,
    tagName: string,
    index: number
  ): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_ATTACH_EXISTING_ELEMENT;
    message[JsonConstants.RPC_NODE] = parent.getId();
    message[JsonConstants.RPC_ATTACH_REQUESTED_ID] = requestedId;
    message[JsonConstants.RPC_ATTACH_ASSIGNED_ID] = assignedId;
    message[JsonConstants.RPC_ATTACH_TAG_NAME] = tagName;
    message[JsonConstants.RPC_ATTACH_INDEX] = index;
    this.#sendMessage(message);
  }

  /**
   * Sends a data for attach existing element with id server side callback.
   *
   * @param parent - parent of the node to attach
   * @param requestedId - originally requested id of a server side node
   * @param assignedId - identifier which should be used on the server side for the
   *          element (instead of requestedId)
   * @param id - id of requested element
   */
  sendExistingElementWithIdAttachToServer(
    parent: StateNode,
    requestedId: number,
    assignedId: number,
    // Java takes a String, and the binding layer reaches this with a null id for
    // an indices-path address, so the null is carried through as Java carries it.
    id: string | null
  ): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_ATTACH_EXISTING_ELEMENT_BY_ID;
    message[JsonConstants.RPC_NODE] = parent.getId();
    message[JsonConstants.RPC_ATTACH_REQUESTED_ID] = requestedId;
    message[JsonConstants.RPC_ATTACH_ASSIGNED_ID] = assignedId;
    message[JsonConstants.RPC_ATTACH_ID] = id;
    this.#sendMessage(message);
  }

  /**
   * Sends a return channel message to the server.
   *
   * @param stateNodeId - the id of the state node that owns the channel.
   * @param channelId - the id of the channel.
   * @param args - array of arguments passed to the channel, not * `null`.
   */
  sendReturnChannelMessage(stateNodeId: number, channelId: number, args: unknown[]): void {
    const message: Record<string, unknown> = {};
    message[JsonConstants.RPC_TYPE] = JsonConstants.RPC_TYPE_CHANNEL;
    message[JsonConstants.RPC_NODE] = stateNodeId;
    message[JsonConstants.RPC_CHANNEL] = channelId;
    message[JsonConstants.RPC_CHANNEL_ARGUMENTS] = args;
    this.#sendMessage(message);
  }

  #sendMessage(message: Record<string, unknown>): void {
    this.#registry
      .getLoadingIndicatorStateHandler()
      .processMessage(
        (message[JsonConstants.RPC_TYPE] as string | undefined) ?? null,
        (message[JsonConstants.RPC_EVENT_TYPE] as string | undefined) ?? null
      );
    const rpcQueue = this.#registry.getServerRpcQueue();
    rpcQueue.add(message);
    rpcQueue.flush();
  }
}
