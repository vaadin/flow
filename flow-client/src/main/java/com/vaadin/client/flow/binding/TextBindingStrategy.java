/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow.binding;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsWeakMap;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Computation;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.client.Browser;
import elemental.dom.Node;
import elemental.dom.Text;

/**
 * Binding strategy for simple (not template) text {@link Node}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class TextBindingStrategy implements BindingStrategy<Text> {

    /**
     * This is used as a weak set. Only keys are important so that they are
     * weakly referenced
     */
    private static final JsWeakMap<StateNode, Boolean> BOUND = JsCollections
            .weakMap();

    @Override
    public Text create(StateNode node) {
        return Browser.getDocument().createTextNode("");
    }

    @Override
    public boolean isApplicable(StateNode node) {
        return node.hasFeature(NodeFeatures.TEXT_NODE);
    }

    @Override
    public void bind(StateNode stateNode, Text htmlNode,
            BinderContext nodeFactory) {
        assert stateNode.hasFeature(NodeFeatures.TEXT_NODE);

        if (BOUND.has(stateNode)) {
            return;
        }
        BOUND.set(stateNode, true);

        NodeMap textMap = stateNode.getMap(NodeFeatures.TEXT_NODE);
        MapProperty textProperty = textMap.getProperty(NodeProperties.TEXT);

        Computation computation = Reactive.runWhenDependenciesChange(
                () -> htmlNode.setData((String) textProperty.getValue()));

        stateNode.addUnregisterListener(e -> unbind(stateNode, computation));

    }

    private void unbind(StateNode node, Computation computation) {
        computation.stop();
        BOUND.delete(node);
    }
}
