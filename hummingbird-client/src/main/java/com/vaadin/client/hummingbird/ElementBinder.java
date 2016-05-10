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
package com.vaadin.client.hummingbird;

import java.util.function.Function;

import com.vaadin.client.hummingbird.nodefeature.NodeList;
import com.vaadin.client.hummingbird.template.TemplateElementBinder;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.dom.Element;
import elemental.dom.Node;

/**
 * Entry point for creating DOM nodes bound to state nodes.
 *
 * @author Vaadin Ltd
 */
public final class ElementBinder {

    private ElementBinder() {
    }

    /**
     * Creates and binds a DOM node for the given state node. For state nodes
     * based on templates, the root element of the template is returned.
     *
     * @param stateNode
     *            the state node for which to create a DOM node, not
     *            <code>null</code>
     * @return the DOM node, not <code>null</code>
     */
    public static Node createAndBind(StateNode stateNode) {
        assert stateNode != null;

        Node node;
        if (stateNode.hasFeature(NodeFeatures.TEXT_NODE)) {
            node = TextElementBinder.createAndBind(stateNode);
        } else if (stateNode.hasFeature(NodeFeatures.TEMPLATE)) {
            node = TemplateElementBinder.createAndBind(stateNode);
        } else if (stateNode.hasFeature(NodeFeatures.ELEMENT_DATA)) {
            node = BasicElementBinder.createAndBind(stateNode);
        } else {
            throw new IllegalArgumentException(
                    "State node has no suitable feature");
        }

        assert node != null;

        stateNode.setDomNode(node);

        return node;
    }

    /**
     * Uses {@link NodeList} feature of the {@code node} identified by
     * {@code featureId} to populate list of nodes. Creates the nodes using
     * {@code nodeFactory} and append them to the {@code parent}.
     * <p>
     * This is just a shorthand for
     * {@link #populateChildren(Element, StateNode, int, Function, Node)} with the
     * {@code null} value for the last parameter
     * 
     * @see #populateChildren(Element, StateNode, int, Function, Node)
     * 
     * @param parent
     *            parent Element, not {@code null}
     * @param node
     *            StateNode to ask a feature for, not {@code null}
     * @param featureId
     *            feature identifier of the {@code node}, not {@code null}
     * @param nodeFactory
     *            node factory which is used to produce an HTML node based on
     *            child StateNode from the feature NodeList, not {@code null}
     * @return the bound children list
     */
    public static NodeList populateChildren(Element parent, StateNode node,
            int featureId, Function<StateNode, Node> nodeFactory) {
        return populateChildren(parent, node, featureId, nodeFactory, null);
    }

    /**
     * Uses {@link NodeList} feature of the {@code node} identified by
     * {@code featureId} to populate list of nodes. Creates the nodes using
     * {@code nodeFactory} and append them to the {@code parent}.
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
     *            feature identifier of the {@code node}, not {@code null}
     * @param nodeFactory
     *            node factory which is used to produce an HTML node based on
     *            child StateNode from the feature NodeList, not {@code null}
     * @param beforeNode
     *            node which is used as a bottom line for added children
     * @return the bound children list
     */
    public static NodeList populateChildren(Element parent, StateNode node,
            int featureId, Function<StateNode, Node> nodeFactory,
            Node beforeNode) {
        NodeList children = node.getList(featureId);

        for (int i = 0; i < children.length(); i++) {
            StateNode childNode = (StateNode) children.get(i);

            Node child = nodeFactory.apply(childNode);

            parent.insertBefore(child, beforeNode);
        }
        return children;
    }

}
