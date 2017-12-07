/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.impl.ShadowRootStateProvider;
import com.vaadin.flow.nodefeature.ShadowRootHost;

/**
 * A class representing a shadow dom root of an element.
 * <p>
 * The root can be created by {@link Element#attachShadow()}.
 *
 * @see Element#attachShadow()
 *
 * @author Vaadin Ltd
 *
 */
public class ShadowRoot extends Node<ShadowRoot> {

    private ShadowRoot(StateNode node) {
        super(node, ShadowRootStateProvider.get());
    }

    /**
     * Gets the shadow root instance mapped to the given state node.
     *
     * @param node
     *            the state node, not <code>null</code>
     * @return the shadow root for the node, not <code>null</code>
     */
    public static ShadowRoot get(StateNode node) {
        assert node != null;
        if (node.hasFeature(ShadowRootHost.class)) {
            return new ShadowRoot(node);
        } else {
            throw new IllegalArgumentException(
                    "Node is not valid as an element");
        }
    }

    @Override
    public Node getParentNode() {
        return null;
    }

    public Element getHost() {
        Node<?> parent = getStateProvider().getParent(getNode());
        assert parent instanceof Element;
        return (Element) parent;
    }

    @Override
    protected ShadowRoot getSelf() {
        return this;
    }

    @Override
    public ShadowRootStateProvider getStateProvider() {
        return (ShadowRootStateProvider) super.getStateProvider();
    }

}
