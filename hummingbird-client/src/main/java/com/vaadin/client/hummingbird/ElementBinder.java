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

import com.vaadin.client.hummingbird.template.TemplateElementBinder;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.dom.Node;

/**
 * Entry point for creating DOM nodes bound to state nodes.
 *
 * @author Vaadin Ltd
 */
public interface ElementBinder {
    /**
     * Creates and binds a DOM node for the given state node. For state nodes
     * based on templates, the root element of the template is returned.
     *
     * @param stateNode
     *            the state node for which to create a DOM node, not
     *            <code>null</code>
     * @return the DOM node, not <code>null</code>
     */
    static Node createAndBind(StateNode stateNode) {
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
}
