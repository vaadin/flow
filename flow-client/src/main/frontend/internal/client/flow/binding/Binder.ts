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

// TypeScript port of com.vaadin.client.flow.binding.Binder. The Java final class
// becomes this function module; it owns the binding-strategy list and the
// BinderContext implementation passed to the strategies so they can create and
// bind child nodes without knowing the other strategies.

/**
 * Entry point for binding Node to state nodes.
 *
 * This is the only public API class for external use.
 */

import { assert } from '../../../assert';
import type { StateNode } from '../StateNode';
import type { BinderContext } from './BinderContext';
import type { BindingStrategy } from './BindingStrategy';
import { SimpleElementBindingStrategy } from './SimpleElementBindingStrategy';
import { TextBindingStrategy } from './TextBindingStrategy';

// The strategies, in order; each handles a different kind of state node. The
// concrete strategies are BindingStrategy<Element> / BindingStrategy<Text>; they
// widen to BindingStrategy<Node> here because the interface's methods use method
// syntax (bivariant parameters), matching the Java raw-typed STRATEGIES array.
const STRATEGIES: Array<BindingStrategy<Node>> = [new SimpleElementBindingStrategy(), new TextBindingStrategy()];

/**
 * Bind the `domNode` to the `stateNode`.
 *
 * @param stateNode - the state node
 * @param domNode - the DOM node to bind, not `null`
 */
export function bind(stateNode: StateNode, domNode: Node): void {
  assert(!stateNode.getTree().isUpdateInProgress(), 'Binding state node while processing state tree changes');

  getApplicableStrategy(stateNode).bind(stateNode, domNode, CONTEXT);
}

/**
 * This is the implementation of {@link BinderContext} which is passed to the
 * {@link BindingStrategy} instances to be able to delegate creation of subnodes
 * with the type that they are not aware of.
 *
 * This is the only factory/binder that may be used inside {@link BindingStrategy}
 * implementation. So that implementation should not know anything about external
 * classes/API. Everything that is required by the {@link BindingStrategy} must be
 * here to avoid uncertainty which methods are allowed/correct to use in the
 * implementation.
 *
 * @see BinderContext
 */
class BinderContextImpl implements BinderContext {
  createAndBind(stateNode: StateNode): Node {
    const strategy = getApplicableStrategy(stateNode);
    let node = stateNode.getDomNode();
    if (node === null) {
      node = strategy.create(stateNode);
      assert(node !== null, 'Binding strategy created a null DOM node');
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

function getApplicableStrategy(node: StateNode): BindingStrategy<Node> {
  let applicable: BindingStrategy<Node> | null = null;
  for (const strategy of STRATEGIES) {
    if (strategy.isApplicable(node)) {
      // Java's message names the two conflicting strategy classes via getClass();
      // the message is built eagerly here, so it stays generic rather than
      // dereferencing the (possibly null) previous strategy to name it.
      assert(applicable === null, 'Found multiple applicable binding strategies for the same node');
      applicable = strategy;
    }
  }
  if (applicable === null) {
    throw new Error('State node has no suitable binder strategy');
  }
  return applicable;
}
