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
import type { BindingStrategy } from './BindingStrategy';

type StateNodeLike = any;
type DomNodeLike = any;

/**
 * Binder context which is passed to the `BindingStrategy` instances enabling
 * them to delegate creation of child nodes. Migrated from
 * `com.vaadin.client.flow.binding.BinderContext`.
 */
export interface BinderContext {
  /**
   * Creates and binds a DOM node for the given state node. For state nodes
   * based on templates, the root element of the template is returned.
   */
  createAndBind(node: StateNodeLike): DomNodeLike;

  /** Binds a DOM node for the given state node. */
  bind(stateNode: StateNodeLike, node: DomNodeLike): void;

  /** Returns the strategies that match the given predicate. */
  getStrategies(predicate: (strategy: BindingStrategy) => boolean): BindingStrategy[];
}
