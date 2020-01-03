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

import java.util.function.Predicate;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.collection.JsArray;

import elemental.dom.Node;

/**
 * Binder context which is passed to the {@link BindingStrategy} instances
 * enabling them to delegate the creation of any child nodes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface BinderContext {

    /**
     * Creates and binds a DOM node for the given state node. For state nodes
     * based on templates, the root element of the template is returned.
     *
     * @param node
     *            the state node for which to create a DOM node, not
     *            <code>null</code>
     * @return the DOM node, not <code>null</code>
     */
    Node createAndBind(StateNode node);

    /**
     * Binds a DOM node for the given state node.
     *
     * @param stateNode
     *            the state node to bind, not {@code null}
     * @param node
     *            the DOM node, not <code>null</code>
     */
    void bind(StateNode stateNode, Node node);

    /**
     * Gets the strategies with a specific type {@code T} using filtering
     * {@code predicate}.
     * <p>
     * Predicate normally should be based on {@code Class<T>#isInstance()} but
     * this method is not available in GWT so predicate {@code instanceof T}
     * should be used. It's the developer responsibility to make sure that the
     * resulting strategies types are correct to avoid
     * {@link ClassCastException}.
     *
     * @param predicate
     *            predicate to filter strategies using type {@code T}.
     * @param <T>
     *            the array type
     * 
     * @return collection of filtered strategies
     */
    <T extends BindingStrategy<?>> JsArray<T> getStrategies(
            Predicate<BindingStrategy<?>> predicate);
}
