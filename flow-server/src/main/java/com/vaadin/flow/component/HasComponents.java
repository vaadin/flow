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
package com.vaadin.flow.component;

import com.vaadin.flow.dom.Element;

/**
 * A component to which the user can add and remove child components.
 * {@link Component} in itself provides basic support for child components that
 * are manually added as children of an element belonging to the component. This
 * interface provides an explicit API for components that explicitly supports
 * adding and removing arbitrary child components.
 * <p>
 * The default implementations assume that children are attached to
 * {@link #getElement()}. Override all methods in this interface if the
 * components should be added to some other element.
 *
 * @author Vaadin Ltd
 */
public interface HasComponents extends HasElement {
    /**
     * Adds the given components as children of this component.
     *
     * @param components
     *            the components to add
     */
    default void add(Component... components) {
        assert components != null;
        for (Component component : components) {
            assert component != null;
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Removes the given child components from this component.
     *
     * @param components
     *            the components to remove
     * @throws IllegalArgumentException
     *             if any of the components is not a child of this component
     */
    default void remove(Component... components) {
        for (Component component : components) {
            assert component != null;
            if (getElement().equals(component.getElement().getParent())) {
                getElement().removeChild(component.getElement());
            } else {
                throw new IllegalArgumentException("The given component ("
                        + component + ") is not a child of this component");
            }
        }
    }

    /**
     * Removes all contents from this component, this includes child components,
     * text content as well as child elements that have been added directly to
     * this component using the {@link Element} API. it also removes the
     * children that were added only at the client-side.
     */
    default void removeAll() {
        getElement().removeAllChildren();
    }
}
