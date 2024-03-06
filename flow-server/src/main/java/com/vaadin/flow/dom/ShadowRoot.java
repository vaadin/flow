/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import com.vaadin.flow.dom.impl.ShadowRootStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ShadowRootHost;

/**
 * Represents a shadow dom root of an element.
 * <p>
 * The root can be created by {@link Element#attachShadow()}.
 *
 * @see Element#attachShadow()
 *
 * @author Vaadin Ltd
 * @since 1.0
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
        if (isShadowRoot(node)) {
            return new ShadowRoot(node);
        } else {
            throw new IllegalArgumentException(
                    "Node is not valid as an element");
        }
    }

    /**
     * Checks whether the given {@code node} is a shadow root node.
     *
     * @param node
     *            the state node, not <code>null</code>
     * @return {@code true} if it's a shadow root, not <code>null</code>
     */
    public static boolean isShadowRoot(StateNode node) {
        return node.hasFeature(ShadowRootHost.class);
    }

    @Override
    public Node<?> getParentNode() {
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
