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

// TypeScript port of the binding-layer contracts
// com.vaadin.client.flow.binding.BindingStrategy and BinderContext, built
// alongside the Java versions. They reference each other, so they live in one
// module. The Java BindingStrategy.getTag default routes through PolymerUtils
// (not ported yet), so it is optional here and wired at cutover.

import type { StateNode } from '../StateNode';

/** Binds a state node to a DOM node of type T; mirrors BindingStrategy.java. */
export interface BindingStrategy<T extends Node> {
  /** Creates a new DOM node for the given state node. */
  create(node: StateNode): T;

  /** Whether this strategy can be applied to the given state node. */
  isApplicable(node: StateNode): boolean;

  /** Binds the state node to the DOM node, using the given context for children. */
  bind(stateNode: StateNode, domNode: T, context: BinderContext): void;

  /** The element tag for the given state node (defaults via PolymerUtils in Java). */
  getTag?(node: StateNode): string;
}

/** Context passed to binding strategies for binding child nodes; mirrors BinderContext.java. */
export interface BinderContext {
  /** Creates a DOM node for the given state node and binds it. */
  createAndBind(node: StateNode): Node;

  /** Binds an already-created DOM node to the given state node. */
  bind(stateNode: StateNode, node: Node): void;

  /** Gets the registered binding strategies matching the given predicate. */
  getStrategies<T extends BindingStrategy<Node>>(predicate: (strategy: BindingStrategy<Node>) => boolean): T[];
}
