/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.binding;

import com.vaadin.client.PolymerUtils;
import com.vaadin.client.flow.StateNode;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.dom.Node;

/**
 * Binding strategy/factory for {@link StateNode}s.
 * <p>
 * Only one strategy may be applicable for the given {@link StateNode} instance.
 * Once the applicable strategy is identified it's used to produce a
 * {@link Node} based on the {@link StateNode} instance and bind it.
 *
 * @param <T>
 *            a DOM node type which strategy is applicable for
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface BindingStrategy<T extends Node> {

    /**
     * Creates a DOM node for the {@code node}.
     *
     * @param node
     *            the state node for which to create a DOM node, not
     *            {@code null}
     * @return the DOM node, not <code>null</code>
     */
    T create(StateNode node);

    /**
     * Returns {@code true} is the strategy is applicable to the {@code node}.
     *
     * @param node
     *            the state node to check against of
     * @return {@code true} if the strategy is applicable to the node
     *
     */
    boolean isApplicable(StateNode node);

    /**
     * Binds a DOM node to the {@code stateNode} using {@code context} to create
     * and bind nodes of other types.
     *
     * @param stateNode
     *            the state node to bind, not {@code null}
     * @param domNode
     *            the DOM node, not <code>null</code>
     * @param context
     *            binder context to create and construct HTML nodes of other
     *            types
     */
    void bind(StateNode stateNode, T domNode, BinderContext context);

    /**
     * Gets the tag value from the {@link NodeFeatures#ELEMENT_DATA} feature for
     * the {@code node}.
     *
     * @param node
     *            the state node
     * @return tag of the {@code node}
     */
    default String getTag(StateNode node) {
        return PolymerUtils.getTag(node);
    }

}
