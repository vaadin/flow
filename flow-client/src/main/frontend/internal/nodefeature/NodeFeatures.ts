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

// Node feature ids, mirroring com.vaadin.flow.internal.nodefeature.NodeFeatures.
export const NodeFeatures = {
  ELEMENT_DATA: 0,
  ELEMENT_PROPERTIES: 1,
  ELEMENT_CHILDREN: 2,
  ELEMENT_ATTRIBUTES: 3,
  ELEMENT_LISTENERS: 4,
  UI_PUSHCONFIGURATION: 5,
  UI_PUSHCONFIGURATION_PARAMETERS: 6,
  TEXT_NODE: 7,
  POLL_CONFIGURATION: 8,
  RECONNECT_DIALOG_CONFIGURATION: 9,
  LOADING_INDICATOR_CONFIGURATION: 10,
  CLASS_LIST: 11,
  ELEMENT_STYLE_PROPERTIES: 12,
  COMPONENT_MAPPING: 15,
  TEMPLATE_MODELLIST: 16,
  POLYMER_SERVER_EVENT_HANDLERS: 17,
  POLYMER_EVENT_LISTENERS: 18,
  CLIENT_DELEGATE_HANDLERS: 19,
  SHADOW_ROOT_DATA: 20,
  SHADOW_ROOT_HOST: 21,
  ATTACH_EXISTING_ELEMENT: 22,
  BASIC_TYPE_VALUE: 23,
  VIRTUAL_CHILDREN: 24,
  RETURN_CHANNEL_MAP: 25,
  INERT_DATA: 26,
  SIGNAL_BINDING: 27
} as const;

// Reserved node property names, mirroring
// com.vaadin.flow.internal.nodefeature.NodeProperties (only the entries the
// ported client code needs so far).
export const NodeProperties = {
  VISIBLE: 'visible'
} as const;
