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
 * A utility class providing constants for JSON related features.
 *
 * TypeScript port of com.vaadin.flow.shared.JsonConstants; a subset containing
 * only the constants the ported communication layer needs so far. Missing
 * entries are added as later ports require them.
 */
export const JsonConstants = {
  /**
   * Key holding id of the node affected by a change.
   */
  CHANGE_NODE: 'node',

  /**
   * Key holding the type of a change.
   */
  CHANGE_TYPE: 'type',

  /**
   * Change type for empty change (populate the feature on the client side
   * only).
   */
  CHANGE_TYPE_NOOP: 'empty',

  /**
   * Change type for attaching nodes.
   */
  CHANGE_TYPE_ATTACH: 'attach',

  /**
   * Change type for detaching nodes.
   */
  CHANGE_TYPE_DETACH: 'detach',

  /**
   * Change type for list splice changes.
   */
  CHANGE_TYPE_SPLICE: 'splice',

  /**
   * Change type for map put changes.
   */
  CHANGE_TYPE_PUT: 'put',

  /**
   * Change type for map remove changes.
   */
  CHANGE_TYPE_REMOVE: 'remove',

  /**
   * Change type for list clear changes.
   */
  CHANGE_TYPE_CLEAR: 'clear',

  /**
   * Key holding the feature of a change.
   */
  CHANGE_FEATURE: 'feat',

  /**
   * Key holding the feature type.
   */
  CHANGE_FEATURE_TYPE: 'featType',

  /**
   * Key holding the map key of the change.
   */
  CHANGE_MAP_KEY: 'key',

  /**
   * Key holding nodes to add for a splice.
   */
  CHANGE_SPLICE_ADD_NODES: 'addNodes',

  /**
   * Key holding values to add for a splice.
   */
  CHANGE_SPLICE_ADD: 'add',

  /**
   * Key holding the number of items to remove for a splice.
   */
  CHANGE_SPLICE_REMOVE: 'remove',

  /**
   * Key holding the index of a splice.
   */
  CHANGE_SPLICE_INDEX: 'index',

  /**
   * Key holding the value of a put change.
   */
  CHANGE_PUT_VALUE: 'value',

  /**
   * Key holder the node value of a put change.
   */
  CHANGE_PUT_NODE_VALUE: 'nodeValue',

  /**
   * Key holding the type in of messages sent from the client.
   */
  RPC_TYPE: 'type',

  /**
   * Type value for events sent from the client.
   */
  RPC_TYPE_EVENT: 'event',

  /**
   * Type value for navigation events from the client.
   */
  RPC_TYPE_NAVIGATION: 'navigation',

  /**
   * Key holding the node in messages sent from the client.
   */
  RPC_NODE: 'node',

  /**
   * Key holding the event type in event messages sent from the client.
   */
  RPC_EVENT_TYPE: 'event',

  /**
   * Type value for model map synchronizations sent from the client.
   */
  RPC_TYPE_MAP_SYNC: 'mSync',

  /**
   * Key holding the event data in event messages sent from the client.
   */
  RPC_EVENT_DATA: 'data',

  /**
   * Key used to hold the feature id when synchronizing node values.
   */
  RPC_FEATURE: 'feature',

  /**
   * Key used to hold the name of the synchronized property.
   */
  RPC_PROPERTY: 'property',

  /**
   * Key used to hold the value of the synchronized property.
   */
  RPC_PROPERTY_VALUE: 'value',

  /**
   * Key used to hold the location in a navigation message.
   */
  RPC_NAVIGATION_LOCATION: 'location',

  /**
   * Key used to hold the state in a navigation message.
   */
  RPC_NAVIGATION_STATE: 'state',

  /**
   * Key used in navigation messages triggered by a router link.
   */
  RPC_NAVIGATION_ROUTERLINK: 'link',

  /**
   * Type value for events sent from the client to an event handler published on
   * the server.
   */
  RPC_PUBLISHED_SERVER_EVENT_HANDLER: 'publishedEventHandler',

  /**
   * Key used to hold the server side method name in template event messages
   * sent from the client.
   */
  RPC_TEMPLATE_EVENT_METHOD_NAME: 'templateEventMethodName',

  /**
   * Key used to hold the argument values for server side method call.
   *
   * @see {@link RPC_TEMPLATE_EVENT_METHOD_NAME}
   * @see {@link RPC_PUBLISHED_SERVER_EVENT_HANDLER}
   */
  RPC_TEMPLATE_EVENT_ARGS: 'templateEventMethodArgs',

  /**
   * Key used to hold the promise id for a server side method call.
   */
  RPC_TEMPLATE_EVENT_PROMISE: 'promise',

  /**
   * Name of the $server property that is used to track pending promises. The
   * name is chosen to avoid conflicts with genuine $server method names.
   */
  RPC_PROMISE_CALLBACK_NAME: '}p',

  /**
   * Type value for attach existing element server callback.
   *
   * @see {@link RPC_ATTACH_ASSIGNED_ID}
   * @see {@link RPC_ATTACH_REQUESTED_ID}
   * @see {@link RPC_ATTACH_TAG_NAME}
   * @see {@link RPC_ATTACH_INDEX}
   */
  RPC_ATTACH_EXISTING_ELEMENT: 'attachExistingElement',

  /**
   * Type value for attach existing element server callback.
   *
   * @see {@link RPC_ATTACH_ASSIGNED_ID}
   * @see {@link RPC_ATTACH_REQUESTED_ID}
   * @see {@link RPC_ATTACH_TAG_NAME}
   * @see {@link RPC_ATTACH_ID}
   */
  RPC_ATTACH_EXISTING_ELEMENT_BY_ID: 'attachExistingElementById',

  /**
   * Key used to hold requested state node identifier for attach existing element
   * request.
   */
  RPC_ATTACH_REQUESTED_ID: 'attachReqId',

  /**
   * Key used to hold assigned state node identifier for attach existing element
   * request.
   */
  RPC_ATTACH_ASSIGNED_ID: 'attachAssignedId',

  /**
   * Key used to hold tag name for attach existing element request.
   */
  RPC_ATTACH_TAG_NAME: 'attachTagName',

  /**
   * Key used to hold index of server side element for attach existing element
   * request.
   */
  RPC_ATTACH_INDEX: 'attachIndex',

  /**
   * Key used to hold id of the element for attach existing element request.
   */
  RPC_ATTACH_ID: 'attachId',

  // "for" is a reserved keyword, which means that this cannot be a valid JS
  // expression, thus eliminating the risk for an accidental collision with a
  // genuine data expression
  /**
   * Key holding the debounce phase for an event data map from the client.
   */
  EVENT_DATA_PHASE: 'for',

  // The `DebouncePhase` references below stay code spans: the enum is
  // com.vaadin.flow.dom.DebouncePhase, server-side and outside this port's scope.
  /**
   * Character used for representing `DebouncePhase.LEADING`.
   */
  EVENT_PHASE_LEADING: 'leading',

  /**
   * Character used for representing `DebouncePhase.INTERMEDIATE`.
   */
  EVENT_PHASE_INTERMEDIATE: 'intermediate',

  /**
   * Character used for representing `DebouncePhase.TRAILING`.
   */
  EVENT_PHASE_TRAILING: 'trailing',

  /**
   * Token used as an event data expression to represent that properties
   * should be synchronized. The token is chosen to avoid collisions with
   * regular event data expressions by using a character that cannot be the
   * start of a valid JS expression.
   */
  SYNCHRONIZE_PROPERTY_TOKEN: '}',

  /**
   * Token used as an event data expression or prefix to an event data
   * expression to represent that the state node ID should be fetched for the
   * element, or its closest parent, that corresponds to `event.target` or the
   * element returned by the evaluated expression.
   *
   * The token is chosen to avoid collisions with regular event data
   * expressions by using a character that cannot be the start of a valid JS
   * expression.
   */
  MAP_STATE_NODE_EVENT_DATA: ']',

  /**
   * RPC type value used for return channel messages.
   */
  RPC_TYPE_CHANNEL: 'channel',

  /**
   * Key for the channel id in return channel messages.
   */
  RPC_CHANNEL: 'channel',

  /**
   * Key for the arguments array in return channel messages.
   */
  RPC_CHANNEL_ARGUMENTS: 'args'
} as const;
