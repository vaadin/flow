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

// TypeScript port of com.vaadin.client.flow.binding.TextBindingStrategy, built
// alongside the Java version on top of the TS reactive core and state node.

import { assert } from '../assert';
import { Reactive, type Computation } from '../reactive/reactive';
import { NodeFeatures, NodeProperties } from '../nodefeature/NodeFeatures';
import type { StateNode } from '../StateNode';
import type { BinderContext, BindingStrategy } from './BindingStrategy';

/** Binding strategy for simple (non-template) text nodes; mirrors TextBindingStrategy.java. */
export class TextBindingStrategy implements BindingStrategy<Text> {
  // Used as a weak set: only keys matter, so state nodes are weakly referenced.
  private static readonly bound = new WeakMap<StateNode, boolean>();

  create(_node: StateNode): Text {
    return document.createTextNode('');
  }

  isApplicable(node: StateNode): boolean {
    return node.hasFeature(NodeFeatures.TEXT_NODE);
  }

  bind(stateNode: StateNode, htmlNode: Text, _context: BinderContext): void {
    assert(stateNode.hasFeature(NodeFeatures.TEXT_NODE), 'Node must have the text feature');
    if (TextBindingStrategy.bound.has(stateNode)) {
      return;
    }
    TextBindingStrategy.bound.set(stateNode, true);

    const textMap = stateNode.getMap(NodeFeatures.TEXT_NODE);
    const textProperty = textMap.getProperty(NodeProperties.TEXT);

    const computation = Reactive.runWhenDependenciesChange(() => {
      htmlNode.data = textProperty.getValue() as string;
    });

    stateNode.addUnregisterListener(() => this.unbind(stateNode, computation));
  }

  private unbind(node: StateNode, computation: Computation): void {
    computation.stop();
    TextBindingStrategy.bound.delete(node);
  }
}
