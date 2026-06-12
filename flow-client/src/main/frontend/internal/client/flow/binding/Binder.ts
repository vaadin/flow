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
import type { BindingStrategy } from './BindingStrategy';
import { SimpleElementBindingStrategyImpl } from './SimpleElementBindingStrategy';
import { TextBindingStrategy } from './TextBindingStrategy';

type StateNodeLike = any;
type DomNodeLike = any;

const STRATEGIES: BindingStrategy[] = [new SimpleElementBindingStrategyImpl(), new TextBindingStrategy()];

/**
 * This is the implementation of `BinderContext` which is passed to the
 * `BindingStrategy` instances so they can delegate creation of subnodes whose
 * type they are not aware of.
 *
 * It is the only factory/binder that may be used inside a `BindingStrategy`
 * implementation. The implementation should not know anything about external
 * classes/API.
 */
const CONTEXT: BinderContext = {
  createAndBind(stateNode: StateNodeLike): DomNodeLike {
    const strategy = getApplicableStrategy(stateNode);
    let node: DomNodeLike = stateNode.getDomNode();
    if (node == null) {
      node = strategy.create(stateNode);
      if (node == null) {
        throw new Error('Strategy returned a null DOM node');
      }
      stateNode.setDomNode(node);
    }
    Binder.bind(stateNode, node);
    return node;
  },

  bind(stateNode: StateNodeLike, node: DomNodeLike): void {
    Binder.bind(stateNode, node);
  },

  getStrategies(predicate: (strategy: BindingStrategy) => boolean): BindingStrategy[] {
    const result: BindingStrategy[] = [];
    for (const strategy of STRATEGIES) {
      if (predicate(strategy)) {
        result.push(strategy);
      }
    }
    return result;
  }
};

function getApplicableStrategy(node: StateNodeLike): BindingStrategy {
  let applicable: BindingStrategy | null = null;
  for (const strategy of STRATEGIES) {
    if (strategy.isApplicable(node)) {
      applicable = strategy;
    }
  }
  if (applicable == null) {
    throw new Error('State node has no suitable binder strategy');
  }
  return applicable;
}

/**
 * Entry point for binding DOM nodes to state nodes. Migrated from
 * `com.vaadin.client.flow.binding.Binder`.
 */
export const Binder = {
  /**
   * Binds the given DOM node to the given state node.
   *
   * @param stateNode the state node
   * @param domNode   the DOM node to bind, not null
   */
  bind(stateNode: StateNodeLike, domNode: DomNodeLike): void {
    if (stateNode.getTree().isUpdateInProgress()) {
      throw new Error('Binding state node while processing state tree changes');
    }
    const applicable = getApplicableStrategy(stateNode);
    applicable.bind(stateNode, domNode, CONTEXT);
  }
};
