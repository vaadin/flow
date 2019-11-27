/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.function.Predicate;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;

import elemental.dom.Node;

/**
 * Entry point for binding Node to state nodes.
 * <p>
 * This is the only public API class for external use.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public final class Binder {

    private static final JsArray<BindingStrategy<?>> STRATEGIES = loadStrategies();

    private static final BinderContext CONTEXT = new BinderContextImpl();

    /**
     * This is the implementation of {@link BinderContext} which is passed to
     * the {@link BindingStrategy} instances to be able to delegate creation of
     * subnodes with the type that they are not aware of.
     * <p>
     * This is the only factory/binder that may be used inside
     * {@link BindingStrategy} implementation. So that implementation should not
     * know anything about external classes/API. Everything that is required by
     * the {@link BindingStrategy} must be here to avoid uncertainty which
     * methods are allowed/correct to use in the implementation.
     *
     * @see BinderContext
     */
    private static class BinderContextImpl implements BinderContext {

        @Override
        public Node createAndBind(StateNode stateNode) {
            BindingStrategy<?> strategy = getApplicableStrategy(stateNode);
            Node node = stateNode.getDomNode();
            if (stateNode.getDomNode() == null) {
                node = strategy.create(stateNode);
                assert node != null;
                stateNode.setDomNode(node);
            }
            bind(stateNode, node);

            return node;
        }

        @Override
        public void bind(StateNode stateNode, Node node) {
            Binder.bind(stateNode, node);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <T extends BindingStrategy<?>> JsArray<T> getStrategies(
                Predicate<BindingStrategy<?>> predicate) {
            JsArray<T> array = JsCollections.array();
            Predicate testFunction = predicate;
            for (int i = 0; i < STRATEGIES.length(); i++) {
                BindingStrategy strategy = STRATEGIES.get(i);
                if (testFunction.test(strategy)) {
                    array.push((T) strategy);
                }
            }
            return array;
        }

    }

    private Binder() {
    }

    /**
     * Bind the {@code domNode} to the {@code stateNode}.
     *
     * @param stateNode
     *            the state node
     * @param domNode
     *            the DOM node to bind, not {@code null}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void bind(StateNode stateNode, Node domNode) {
        assert !stateNode.getTree()
                .isUpdateInProgress() : "Binding state node while processing state tree changes";

        BindingStrategy applicable = getApplicableStrategy(stateNode);
        applicable.bind(stateNode, domNode, CONTEXT);
    }

    private static BindingStrategy<?> getApplicableStrategy(StateNode node) {
        BindingStrategy<?> applicable = null;
        for (int i = 0; i < STRATEGIES.length(); i++) {
            BindingStrategy<?> strategy = STRATEGIES.get(i);
            if (strategy.isApplicable(node)) {
                assert applicable == null : "Found two strategies for the node : "
                        + applicable.getClass() + ", " + strategy.getClass();
                applicable = strategy;
            }
        }
        if (applicable == null) {
            throw new IllegalArgumentException(
                    "State node has no suitable binder strategy");
        }
        return applicable;
    }

    private static JsArray<BindingStrategy<?>> loadStrategies() {
        JsArray<BindingStrategy<?>> array = JsCollections.array();
        array.push(new SimpleElementBindingStrategy());
        array.push(new TextBindingStrategy());

        return array;
    }

}
