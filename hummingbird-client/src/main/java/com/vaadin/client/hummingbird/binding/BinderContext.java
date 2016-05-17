/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird.binding;

import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.nodefeature.NodeList;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Binder context which is passed to the {@link BindingStrategy} instances
 * enabling them to delegate the creation of any child nodes.
 * 
 * @author Vaadin Ltd
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
     * @return collection of filtered strategies
     */
    <T extends BindingStrategy<?>> JsArray<T> getStrategies(
            Predicate<BindingStrategy<?>> predicate);

    /**
     * Populates a list of child nodes and appends them into the given element.
     * The children are fetched from the given {@link StateNode}'s
     * {@link NodeList}, found with the given featureId. Nodes are created using
     * the given factory.
     * <p>
     * This is just a shorthand for
     * {@link #populateChildren(Element, StateNode, int, Function, Node)} with
     * the {@code null} value for the last parameter
     * 
     * @see #populateChildren(Element, StateNode, int, Function, Node)
     * 
     * @param parent
     *            parent Element, not {@code null}
     * @param node
     *            StateNode to ask a feature for, not {@code null}
     * @param featureId
     *            feature identifier of the {@code node}
     * @param factory
     *            node factory which is used to produce and bind an HTML node
     *            based on child StateNode from the feature NodeList, not
     *            {@code null}
     * @return the bound children list
     */
    default NodeList populateChildren(Element parent, StateNode node,
            int featureId, Function<StateNode, Node> factory) {
        return populateChildren(parent, node, featureId, factory, null);
    }

    /**
     * Populates a list of child nodes and appends them into the given element.
     * The children are fetched from the given {@link StateNode}'s
     * {@link NodeList}, found with the given featureId. Nodes are created using
     * the given factory.
     * <p>
     * The {@code beforeNode} parameter is used to add children to the
     * {@code parent} before the {@code beforeNode}. It can be {@code null}.
     * 
     * @see #populateChildren(Element, StateNode, int, Function)
     * 
     * @param parent
     *            parent Element, not {@code null}
     * @param node
     *            StateNode to ask a feature for, not {@code null}
     * @param featureId
     *            feature identifier of the {@code node}
     * @param factory
     *            node factory which is used to produce and bind an HTML node
     *            based on child StateNode from the feature NodeList, not
     *            {@code null}
     * @param beforeNode
     *            node which is used as a bottom line for added children
     * @return the bound children list
     */
    default NodeList populateChildren(Element parent, StateNode node,
            int featureId, Function<StateNode, Node> factory, Node beforeNode) {
        assert node != null;
        assert parent != null;
        assert factory != null;
        NodeList children = node.getList(featureId);

        for (int i = 0; i < children.length(); i++) {
            StateNode childNode = (StateNode) children.get(i);

            Node child = factory.apply(childNode);

            parent.insertBefore(child, beforeNode);
        }
        return children;
    }
}
