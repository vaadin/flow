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

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.shared.Namespaces;
import com.vaadin.ui.Component;

/**
 * Namespace for basic element information.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementDataNamespace extends MapNamespace {

    private Component component = null;

    /**
     * Creates a new element data namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     *
     */
    public ElementDataNamespace(StateNode node) {
        super(node);
    }

    /**
     * Sets the tag name of the element.
     *
     * @param tag
     *            the tag name
     */
    public void setTag(String tag) {
        put(Namespaces.TAG, tag);
    }

    /**
     * Gets the tag name of the element.
     *
     * @return the tag name
     */
    public String getTag() {
        return (String) get(Namespaces.TAG);
    }

    /**
     * Assigns the given component to this node or removes the component if the
     * parameter is null.
     * <p>
     * When assigning a component to the node, there must be no previously
     * assigned component, i.e. you cannot replace one component with another
     * using this method.
     *
     * @param component
     *            the component to assign to this node
     */
    public void setComponent(Component component) {
        assert (component == null)
                || (this.component == null) : "Cannot replace the component using setComponent";

        this.component = component;
    }

    /**
     * Gets the component assigned to this node, if any.
     *
     * @return an optional containing the component or {@link Optional#empty()}
     *         if no component has been assigned to this element
     */
    public Optional<Component> getComponent() {
        return Optional.ofNullable(component);
    }

}
