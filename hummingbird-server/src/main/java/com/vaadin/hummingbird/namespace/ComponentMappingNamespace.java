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
package com.vaadin.hummingbird.namespace;

import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.ui.Component;

/**
 * A server side only namespace for mapping a node to a component.
 *
 * @author Vaadin
 * @since
 */
public class ComponentMappingNamespace extends Namespace {

    private Component component = null;

    /**
     * Creates an instance of this namespace.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    protected ComponentMappingNamespace(StateNode node) {
        super(node);
    }

    /**
     * Assigns the given component to this node.
     * <p>
     * When assigning a component to the node, there must be no previously
     * assigned component.
     *
     * @param component
     *            the component to assign to this node, not {@code null}
     */
    public void setComponent(Component component) {
        assert component != null : "Component must not be null";
        this.component = component;
    }

    /**
     * Gets the component this node has been mapped to, if any.
     *
     * @return an optional component, or an empty optional if no component has
     *         been mapped to this node
     */
    public Optional<Component> getComponent() {
        return Optional.ofNullable(component);
    }

    @Override
    public void collectChanges(Consumer<NodeChange> collector) {
        // Server side only namespace
    }

    @Override
    public void resetChanges() {
        // Server side only namespace
    }

    @Override
    public void forEachChild(Consumer<StateNode> action) {
        // Server side only namespace
    }

}
