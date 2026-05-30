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
import { Computation } from '../reactive/Computation';
import type { BinderContext } from './BinderContext';
import type { BindingStrategy } from './BindingStrategy';

// Mirrors com.vaadin.flow.internal.nodefeature.NodeFeatures.TEXT_NODE.
const TEXT_NODE = 7;
// Mirrors com.vaadin.flow.internal.nodefeature.NodeProperties.TEXT.
const TEXT_PROPERTY = 'text';

type StateNodeLike = any;

// Weak set of bound state nodes; entries are GC'd along with the state node.
const BOUND = new WeakMap<object, boolean>();

/**
 * Binding strategy for simple (not template) text nodes. Migrated from
 * `com.vaadin.client.flow.binding.TextBindingStrategy`.
 */
export class TextBindingStrategy implements BindingStrategy<Text> {
  create(_node: StateNodeLike): Text {
    return document.createTextNode('');
  }

  isApplicable(node: StateNodeLike): boolean {
    return node.hasFeature(TEXT_NODE);
  }

  bind(stateNode: StateNodeLike, htmlNode: Text, _nodeFactory: BinderContext): void {
    if (BOUND.has(stateNode)) {
      return;
    }
    BOUND.set(stateNode, true);

    const textMap = stateNode.getMap(TEXT_NODE);
    const textProperty = textMap.getProperty(TEXT_PROPERTY);

    const computation = new Computation(() => {
      htmlNode.data = textProperty.getValue() as string;
    });

    stateNode.addUnregisterListener(() => {
      computation.stop();
      BOUND.delete(stateNode);
    });
  }
}
