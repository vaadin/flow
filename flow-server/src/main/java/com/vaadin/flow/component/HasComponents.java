/*
 * Copyright 2000-2026 Vaadin Ltd.
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

/**
 * A component to which the user can add and remove child components.
 * {@link Component} in itself provides basic support for child components that
 * are manually added as children of an element belonging to the component. This
 * interface provides an explicit API for components that explicitly support
 * adding and removing arbitrary child components.
 * <p>
 * {@link HasComponents} is generally implemented by layouts or components whose
 * primary function is to host child components. It isn't, for example,
 * implemented by non-layout components such as fields.
 * <p>
 * This interface is equivalent to {@code HasComponentsOfType<Component>}; if
 * the container should only accept a restricted set of child component types,
 * implement {@link HasComponentsOfType} directly with that type instead.
 * <p>
 * The default implementations assume that children are attached to
 * {@link #getElement()}. Override all methods in this interface if the
 * components should be added to some other element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface HasComponents extends HasComponentsOfType<Component> {

    /**
     * Add the given text as a child of this component.
     *
     * @param text
     *            the text to add, not <code>null</code>
     */
    default void add(String text) {
        add(new Text(text));
    }
}
