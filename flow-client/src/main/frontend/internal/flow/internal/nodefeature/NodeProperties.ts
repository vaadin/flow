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
 * Various node properties' ids.
 *
 * For internal use only. May be renamed or removed in a future release.
 *
 * TypeScript port of com.vaadin.flow.internal.nodefeature.NodeProperties,
 * containing only the entries the ported client code needs so far. Absent Java
 * keys (e.g. `ID`) are added as later ports start to require them.
 *
 * The per-member Javadoc below references server-side classes (e.g.
 * `ElementData`, `VirtualChildrenList`) from `com.vaadin.flow.internal.nodefeature`,
 * outside this port's scope, so those references stay code spans permanently.
 */
export const NodeProperties = {
  /**
   * Key for `ElementData#getTag()`.
   */
  TAG: 'tag',

  /**
   * Key for `ElementData#getNamespace()`.
   */
  NAMESPACE: 'namespace',

  /**
   * Key for `ElementData#getPayload()`.
   */
  PAYLOAD: 'payload',

  /**
   * Key for `ElementData#getJavaClass()`.
   */
  JAVA_CLASS: 'jc',

  /**
   * Key for `TextNodeMap#getText()`.
   */
  TEXT: 'text',

  /**
   * Key for `ShadowRootData`.
   */
  SHADOW_ROOT: 'shadowRoot',

  /**
   * Key for `BasicTypeValue#getValue()`.
   */
  VALUE: 'value',

  /**
   * JsonObject type key for `VirtualChildrenList`.
   */
  TYPE: 'type',

  /**
   * JsonObject in-memory type value for `VirtualChildrenList`.
   */
  IN_MEMORY_CHILD: 'inMemory',

  /**
   * JsonObject `@id` type value for `VirtualChildrenList`.
   */
  INJECT_BY_ID: '@id',

  /**
   * JsonObject `@name` type value for `VirtualChildrenList`.
   */
  INJECT_BY_NAME: '@name',

  /**
   * JsonObject template-in-template type value for `VirtualChildrenList`.
   */
  TEMPLATE_IN_TEMPLATE: 'subTemplate',

  /**
   * Key for `ElementData#isVisible()`.
   */
  VISIBLE: 'visible',

  /**
   * The property value used on the client side only in addition to
   * {@link NodeProperties.VISIBLE}.
   */
  VISIBILITY_BOUND_PROPERTY: 'bound',

  /**
   * The property used on the client side only in addition to
   * {@link NodeProperties.VISIBLE}. Stores the client side value of "hidden"
   * property.
   */
  VISIBILITY_HIDDEN_PROPERTY: 'hidden',

  /**
   * The property used on the client side only in addition to
   * {@link NodeProperties.VISIBLE}. It stores the client side value of the CSS
   * "display" property to be able to restore when making a hidden element
   * visible again. Used only when the element is inside a shadow root, and the
   * CSS "display: none" is set in addition the "hidden" attribute.
   */
  VISIBILITY_STYLE_DISPLAY_PROPERTY: 'styleDisplay',

  /**
   * The property in Json object which marks the object as special value
   * transmitting URI (not just any string).
   *
   * Used in the `ElementAttributeMap`.
   */
  URI_ATTRIBUTE: 'uri',

  /**
   * The "slot" attribute, which should be sent to the client and applied to
   * the DOM element even when the element is initially invisible. This is a
   * structural attribute needed for CSS selectors and layout.
   */
  SLOT_ATTRIBUTE: 'slot'
} as const;
