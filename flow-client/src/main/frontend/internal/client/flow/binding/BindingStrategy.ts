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
import type { BinderContext } from './BinderContext';

type StateNodeLike = any;
type DomNodeLike = any;

/**
 * Binding strategy/factory for `StateNode`s. Migrated from
 * `com.vaadin.client.flow.binding.BindingStrategy`.
 *
 * Only one strategy may be applicable for a given `StateNode`. The applicable
 * strategy produces a DOM node for the state node and binds the two together.
 */
export interface BindingStrategy<T = DomNodeLike> {
  /** Creates a DOM node for the given state node. */
  create(node: StateNodeLike): T;

  /** Returns `true` if the strategy is applicable to the given state node. */
  isApplicable(node: StateNodeLike): boolean;

  /**
   * Binds a DOM node to the state node, using `context` to create and bind
   * child nodes of other types.
   */
  bind(stateNode: StateNodeLike, domNode: T, context: BinderContext): void;
}
