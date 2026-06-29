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

// TypeScript port of com.vaadin.client.flow.binding.Binder, the entry point for
// binding DOM nodes to state nodes, built alongside the Java version. It owns
// the binding-strategy list and the BinderContext implementation passed to the
// strategies so they can create and bind child nodes without knowing the other
// strategies.

import { assert } from '../assert';
import type { StateNode } from '../StateNode';
import type { BinderContext, BindingStrategy } from './BindingStrategy';
import { SimpleElementBindingStrategy } from './SimpleElementBindingStrategy';
import { TextBindingStrategy } from './TextBindingStrategy';

// The strategies, in order; each handles a different kind of state node. The
// generic-variance mismatch on bind() (T is contravariant) is bridged with a
// cast, matching the Java raw-typed STRATEGIES array.
const STRATEGIES: Array<BindingStrategy<Node>> = [
  new SimpleElementBindingStrategy() as unknown as BindingStrategy<Node>,
  new TextBindingStrategy() as unknown as BindingStrategy<Node>
];

function getApplicableStrategy(node: StateNode): BindingStrategy<Node> {
  let applicable: BindingStrategy<Node> | null = null;
  for (const strategy of STRATEGIES) {
    if (strategy.isApplicable(node)) {
      assert(applicable === null, 'Found multiple applicable binding strategies for the same node');
      applicable = strategy;
    }
  }
  if (applicable === null) {
    throw new Error('State node has no suitable binder strategy');
  }
  return applicable;
}

/**
 * Binds the DOM node to the state node using the applicable strategy. Mirrors
 * Binder.bind.
 */
export function bind(stateNode: StateNode, domNode: Node): void {
  getApplicableStrategy(stateNode).bind(stateNode, domNode, CONTEXT);
}

// The BinderContext passed to strategies; mirrors Binder.BinderContextImpl.
class BinderContextImpl implements BinderContext {
  createAndBind(stateNode: StateNode): Node {
    const strategy = getApplicableStrategy(stateNode);
    let node = stateNode.getDomNode();
    if (node === null) {
      node = strategy.create(stateNode);
      stateNode.setDomNode(node);
    }
    bind(stateNode, node);
    return node;
  }

  bind(stateNode: StateNode, node: Node): void {
    bind(stateNode, node);
  }

  getStrategies<T extends BindingStrategy<Node>>(predicate: (strategy: BindingStrategy<Node>) => boolean): T[] {
    return STRATEGIES.filter(predicate) as T[];
  }
}

const CONTEXT: BinderContext = new BinderContextImpl();
