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

// Shared client/server JSON message constants, mirroring
// com.vaadin.flow.shared.JsonConstants (the subset the ported communication
// layer needs so far).
export const JsonConstants = {
  RPC_TYPE: 'type',
  RPC_TYPE_EVENT: 'event',
  RPC_TYPE_NAVIGATION: 'navigation',
  RPC_TYPE_MAP_SYNC: 'mSync',
  RPC_TYPE_CHANNEL: 'channel',
  RPC_NODE: 'node',
  RPC_EVENT_TYPE: 'event',
  RPC_EVENT_DATA: 'data',
  RPC_FEATURE: 'feature',
  RPC_PROPERTY: 'property',
  RPC_PROPERTY_VALUE: 'value',
  RPC_NAVIGATION_LOCATION: 'location',
  RPC_NAVIGATION_STATE: 'state',
  RPC_NAVIGATION_ROUTERLINK: 'link',
  RPC_PUBLISHED_SERVER_EVENT_HANDLER: 'publishedEventHandler',
  RPC_TEMPLATE_EVENT_METHOD_NAME: 'templateEventMethodName',
  RPC_TEMPLATE_EVENT_ARGS: 'templateEventMethodArgs',
  RPC_TEMPLATE_EVENT_PROMISE: 'promise',
  RPC_ATTACH_EXISTING_ELEMENT: 'attachExistingElement',
  RPC_ATTACH_EXISTING_ELEMENT_BY_ID: 'attachExistingElementById',
  RPC_ATTACH_REQUESTED_ID: 'attachReqId',
  RPC_ATTACH_ASSIGNED_ID: 'attachAssignedId',
  RPC_ATTACH_TAG_NAME: 'attachTagName',
  RPC_ATTACH_INDEX: 'attachIndex',
  RPC_ATTACH_ID: 'attachId',
  RPC_CHANNEL: 'channel',
  RPC_CHANNEL_ARGUMENTS: 'args'
} as const;
