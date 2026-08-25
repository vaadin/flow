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

// TypeScript port of com.vaadin.client.flow.binding.BindingStrategy, built
// alongside the Java version.
//
// The Java `getTag` default method delegates to the (now ported)
// PolymerUtils.getTag. A TypeScript interface cannot carry a method body, so
// `getTag` is declared optional here and the delegation is done by the
// implementing strategy (see SimpleElementBindingStrategy).

import type { StateNode } from '../StateNode';
import type { BinderContext } from './BinderContext';

/**
 * Binding strategy/factory for {@link StateNode}s.
 *
 * Only one strategy may be applicable for the given {@link StateNode} instance.
 * Once the applicable strategy is identified it's used to produce a `Node` based
 * on the {@link StateNode} instance and bind it.
 *
 * @typeParam T - a DOM node type which strategy is applicable for
 */
export interface BindingStrategy<T extends Node> {
  /**
   * Creates a DOM node for the `node`.
   *
   * @param node - the state node for which to create a DOM node, not `null`
   * @returns the DOM node, not `null`
   */
  create(node: StateNode): T;

  /**
   * Returns `true` is the strategy is applicable to the `node`.
   *
   * @param node - the state node to check against of
   * @returns `true` if the strategy is applicable to the node
   */
  isApplicable(node: StateNode): boolean;

  /**
   * Binds a DOM node to the `stateNode` using `context` to create and bind
   * nodes of other types.
   *
   * @param stateNode - the state node to bind, not `null`
   * @param domNode - the DOM node, not `null`
   * @param context - binder context to create and construct HTML nodes of other
   *            types
   */
  bind(stateNode: StateNode, domNode: T, context: BinderContext): void;

  /**
   * Gets the tag value from the {@link NodeFeatures.ELEMENT_DATA} feature for
   * the `node`.
   *
   * @param node - the state node
   * @returns tag of the `node`
   */
  getTag?(node: StateNode): string | null;
}
