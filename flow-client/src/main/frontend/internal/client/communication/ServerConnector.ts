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

// Mirrors com.vaadin.flow.shared.JsonConstants.* RPC-message keys/values.
const RPC_TYPE = 'type';
const RPC_TYPE_EVENT = 'event';
const RPC_TYPE_NAVIGATION = 'navigation';
const RPC_NODE = 'node';
const RPC_EVENT_TYPE = 'event';
const RPC_TYPE_MAP_SYNC = 'mSync';
const RPC_EVENT_DATA = 'data';
const RPC_FEATURE = 'feature';
const RPC_PROPERTY = 'property';
const RPC_PROPERTY_VALUE = 'value';
const RPC_NAVIGATION_LOCATION = 'location';
const RPC_NAVIGATION_STATE = 'state';
const RPC_NAVIGATION_ROUTERLINK = 'link';
const RPC_PUBLISHED_SERVER_EVENT_HANDLER = 'publishedEventHandler';
const RPC_TEMPLATE_EVENT_METHOD_NAME = 'templateEventMethodName';
const RPC_TEMPLATE_EVENT_ARGS = 'templateEventMethodArgs';
const RPC_TEMPLATE_EVENT_PROMISE = 'promise';
const RPC_ATTACH_EXISTING_ELEMENT = 'attachExistingElement';
const RPC_ATTACH_EXISTING_ELEMENT_BY_ID = 'attachExistingElementById';
const RPC_ATTACH_REQUESTED_ID = 'attachReqId';
const RPC_ATTACH_ASSIGNED_ID = 'attachAssignedId';
const RPC_ATTACH_TAG_NAME = 'attachTagName';
const RPC_ATTACH_INDEX = 'attachIndex';
const RPC_ATTACH_ID = 'attachId';
const RPC_TYPE_CHANNEL = 'channel';
const RPC_CHANNEL = 'channel';
const RPC_CHANNEL_ARGUMENTS = 'args';

type StateNodeLike = { getId(): number };

type ServerRpcQueueLike = {
  add(invocation: unknown): void;
  flush(): void;
};

type LoadingIndicatorStateHandlerLike = {
  processMessage(rpcType: string, eventType: string | null): void;
};

/**
 * Constructs RPC messages for the server and queues them via
 * `ServerRpcQueue`. Migrated from
 * `com.vaadin.client.communication.ServerConnector`.
 *
 * `loadingIndicatorStateHandler` and `rpcQueue` are wired at construction so
 * the TS class does not dispatch back through the Java Registry facade.
 *
 * `encodeWithoutTypeInfo` is inlined as a passthrough: in JS the primitive
 * value the Java callers pass (String / Number / Boolean / null / JsonValue)
 * is already a JSON-compatible value, so no wrapping is needed.
 */
export class ServerConnector {
  private readonly loadingIndicatorStateHandler: LoadingIndicatorStateHandlerLike;
  private readonly rpcQueue: ServerRpcQueueLike;

  constructor(loadingIndicatorStateHandler: LoadingIndicatorStateHandlerLike, rpcQueue: ServerRpcQueueLike) {
    this.loadingIndicatorStateHandler = loadingIndicatorStateHandler;
    this.rpcQueue = rpcQueue;
  }

  sendNavigationMessage(location: string, stateObject: unknown, routerLinkEvent: boolean): void {
    const message: Record<string, unknown> = {};
    message[RPC_TYPE] = RPC_TYPE_NAVIGATION;
    message[RPC_NAVIGATION_LOCATION] = location;
    if (stateObject !== null && stateObject !== undefined) {
      message[RPC_NAVIGATION_STATE] = stateObject;
    }
    if (routerLinkEvent) {
      // Only presence of the key is checked; use any short value.
      message[RPC_NAVIGATION_ROUTERLINK] = 1;
    }
    this.sendMessage(message);
  }

  sendEventMessage(node: StateNodeLike, eventType: string, eventData: unknown): void {
    this.sendEventMessageByNodeId(node.getId(), eventType, eventData);
  }

  sendEventMessageByNodeId(nodeId: number, eventType: string, eventData: unknown): void {
    const message: Record<string, unknown> = {};
    message[RPC_TYPE] = RPC_TYPE_EVENT;
    message[RPC_NODE] = nodeId;
    message[RPC_EVENT_TYPE] = eventType;
    if (eventData !== null && eventData !== undefined) {
      message[RPC_EVENT_DATA] = eventData;
    }
    this.sendMessage(message);
  }

  sendTemplateEventMessage(node: StateNodeLike, methodName: string, argsArray: unknown[], promiseId: number): void {
    const message: Record<string, unknown> = {};
    message[RPC_TYPE] = RPC_PUBLISHED_SERVER_EVENT_HANDLER;
    message[RPC_NODE] = node.getId();
    message[RPC_TEMPLATE_EVENT_METHOD_NAME] = methodName;
    message[RPC_TEMPLATE_EVENT_ARGS] = argsArray;
    if (promiseId !== -1) {
      message[RPC_TEMPLATE_EVENT_PROMISE] = promiseId;
    }
    this.sendMessage(message);
  }

  sendNodeSyncMessage(node: StateNodeLike, feature: number, key: string, value: unknown): void {
    const message: Record<string, unknown> = {};
    message[RPC_TYPE] = RPC_TYPE_MAP_SYNC;
    message[RPC_NODE] = node.getId();
    message[RPC_FEATURE] = feature;
    message[RPC_PROPERTY] = key;
    message[RPC_PROPERTY_VALUE] = value === undefined ? null : value;
    this.sendMessage(message);
  }

  // eslint-disable-next-line @typescript-eslint/max-params
  sendExistingElementAttachToServer(
    parent: StateNodeLike,
    requestedId: number,
    assignedId: number,
    tagName: string,
    index: number
  ): void {
    const message: Record<string, unknown> = {};
    message[RPC_TYPE] = RPC_ATTACH_EXISTING_ELEMENT;
    message[RPC_NODE] = parent.getId();
    message[RPC_ATTACH_REQUESTED_ID] = requestedId;
    message[RPC_ATTACH_ASSIGNED_ID] = assignedId;
    message[RPC_ATTACH_TAG_NAME] = tagName;
    message[RPC_ATTACH_INDEX] = index;
    this.sendMessage(message);
  }

  sendExistingElementWithIdAttachToServer(
    parent: StateNodeLike,
    requestedId: number,
    assignedId: number,
    id: string
  ): void {
    const message: Record<string, unknown> = {};
    message[RPC_TYPE] = RPC_ATTACH_EXISTING_ELEMENT_BY_ID;
    message[RPC_NODE] = parent.getId();
    message[RPC_ATTACH_REQUESTED_ID] = requestedId;
    message[RPC_ATTACH_ASSIGNED_ID] = assignedId;
    message[RPC_ATTACH_ID] = id;
    this.sendMessage(message);
  }

  sendReturnChannelMessage(stateNodeId: number, channelId: number, args: unknown[]): void {
    const message: Record<string, unknown> = {};
    message[RPC_TYPE] = RPC_TYPE_CHANNEL;
    message[RPC_NODE] = stateNodeId;
    message[RPC_CHANNEL] = channelId;
    message[RPC_CHANNEL_ARGUMENTS] = args;
    this.sendMessage(message);
  }

  private sendMessage(message: Record<string, unknown>): void {
    const eventType = typeof message[RPC_EVENT_TYPE] === 'string' ? (message[RPC_EVENT_TYPE] as string) : null;
    this.loadingIndicatorStateHandler.processMessage(message[RPC_TYPE] as string, eventType);
    this.rpcQueue.add(message);
    this.rpcQueue.flush();
  }
}
