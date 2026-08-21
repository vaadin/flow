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
 * Registry of node feature id numbers and map keys shared between server and
 * client.
 *
 * For internal use only. May be renamed or removed in a future release.
 *
 * TypeScript port of com.vaadin.flow.internal.nodefeature.NodeFeatures.
 *
 * TODO(flow-client-ts): each id's Javadoc below references the server-side
 * feature class it identifies (e.g. `ElementData`), none of which are ported to
 * TypeScript yet. They are kept as code spans for now; restore them as
 * `{@link ...}` links once those classes are available in follow-up PRs.
 */
export const NodeFeatures = {
  /**
   * Id for `ElementData`.
   */
  ELEMENT_DATA: 0,

  /**
   * Id for `ElementPropertyMap`.
   */
  ELEMENT_PROPERTIES: 1,

  /**
   * Id for `ElementChildrenList`.
   */
  ELEMENT_CHILDREN: 2,

  /**
   * Id for `ElementAttributeMap`.
   */
  ELEMENT_ATTRIBUTES: 3,

  /**
   * Id for `ElementListenerMap`.
   */
  ELEMENT_LISTENERS: 4,

  /**
   * Id for `PushConfigurationMap`.
   */
  UI_PUSHCONFIGURATION: 5,

  /**
   * Id for `PushConfigurationParametersMap`.
   */
  UI_PUSHCONFIGURATION_PARAMETERS: 6,

  /**
   * Id for `TextNodeMap`.
   */
  TEXT_NODE: 7,

  /**
   * Id for `PollConfigurationMap`.
   */
  POLL_CONFIGURATION: 8,

  /**
   * Id for `ReconnectDialogConfigurationMap`.
   */
  RECONNECT_DIALOG_CONFIGURATION: 9,

  /**
   * Id for `ReconnectDialogConfigurationMap`.
   */
  LOADING_INDICATOR_CONFIGURATION: 10,

  /**
   * Id for `ElementClassList`.
   */
  CLASS_LIST: 11,

  /**
   * Id for `ElementStylePropertyMap`.
   */
  ELEMENT_STYLE_PROPERTIES: 12,

  /**
   * Id for `ComponentMapping`.
   */
  COMPONENT_MAPPING: 15,

  /**
   * Id for `ModelList`.
   */
  TEMPLATE_MODELLIST: 16,

  /**
   * Id for `PolymerServerEventHandlers`.
   */
  POLYMER_SERVER_EVENT_HANDLERS: 17,

  /**
   * Id for `PolymerEventListenerMap`.
   */
  POLYMER_EVENT_LISTENERS: 18,

  /**
   * Id for `ClientCallableHandlers`.
   */
  CLIENT_DELEGATE_HANDLERS: 19,

  /**
   * Id for `ShadowRootData`.
   */
  SHADOW_ROOT_DATA: 20,

  /**
   * Id for `ShadowRootHost`.
   */
  SHADOW_ROOT_HOST: 21,

  /**
   * Id for `AttachExistingElementFeature`.
   */
  ATTACH_EXISTING_ELEMENT: 22,

  /**
   * Id for `BasicTypeValue`.
   */
  BASIC_TYPE_VALUE: 23,

  /**
   * Id for `VirtualChildrenList`.
   */
  VIRTUAL_CHILDREN: 24,

  /**
   * Id for `ReturnChannelMap`.
   */
  RETURN_CHANNEL_MAP: 25,

  /**
   * Id for `InertData`.
   */
  INERT_DATA: 26,

  /**
   * Id for `SignalBindingFeature`.
   */
  SIGNAL_BINDING: 27
} as const;
