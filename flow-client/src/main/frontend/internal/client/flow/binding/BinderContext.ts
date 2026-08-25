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

// TypeScript port of com.vaadin.client.flow.binding.BinderContext, built
// alongside the Java version.

import type { StateNode } from '../StateNode';
import type { BindingStrategy } from './BindingStrategy';

/**
 * Binder context which is passed to the {@link BindingStrategy} instances
 * enabling them to delegate the creation of any child nodes.
 */
export interface BinderContext {
  /**
   * Creates and binds a DOM node for the given state node. For state nodes
   * based on templates, the root element of the template is returned.
   *
   * @param node - the state node for which to create a DOM node, not `null`
   * @returns the DOM node, not `null`
   */
  createAndBind(node: StateNode): Node;

  /**
   * Binds a DOM node for the given state node.
   *
   * @param stateNode - the state node to bind, not `null`
   * @param node - the DOM node, not `null`
   */
  bind(stateNode: StateNode, node: Node): void;

  /**
   * Gets the strategies with a specific type `T` using filtering `predicate`.
   *
   * Predicate normally should be based on `Class<T>#isInstance()` but this
   * method is not available in GWT so predicate `instanceof T` should be used.
   * It's the developer responsibility to make sure that the resulting
   * strategies types are correct to avoid `ClassCastException`.
   *
   * @param predicate - predicate to filter strategies using type `T`.
   * @returns collection of filtered strategies
   */
  getStrategies<T extends BindingStrategy<Node>>(predicate: (strategy: BindingStrategy<Node>) => boolean): T[];
}
