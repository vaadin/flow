/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
