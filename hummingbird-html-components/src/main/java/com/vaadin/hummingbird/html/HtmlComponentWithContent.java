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
package com.vaadin.hummingbird.html;

import com.vaadin.annotations.Tag;
import com.vaadin.ui.Component;

/**
 * Base class for a {@link Component} that represents a single built-in HTML
 * element that can contain child components or text.
 *
 * @since
 * @author Vaadin Ltd
 */
public class HtmlComponentWithContent extends HtmlComponent {
    /**
     * Creates an empty component with the element determined by the {@link Tag}
     * annotation of a sub class.
     */
    protected HtmlComponentWithContent() {
        super();
    }

    /**
     * Creates a component with the given child components. The element is
     * determined by the {@link Tag} annotation of a sub class.
     *
     * @param components
     *            the child components
     */
    protected HtmlComponentWithContent(Component... components) {
        add(components);
    }

    /**
     * Creates a new empty component with a new element with the given tag name.
     *
     * @param tagName
     *            the tag name of the element to use for this component, not
     *            <code>null</code>
     */
    public HtmlComponentWithContent(String tagName) {
        super(tagName);
    }

    /**
     * Creates a new component with the given contents and a new element with
     * the given tag name.
     *
     * @param tagName
     *            the tag name of the element to use for this component, not
     *            <code>null</code>
     * @param components
     *            the child components
     */
    public HtmlComponentWithContent(String tagName, Component... components) {
        super(tagName);
        add(components);
    }

    /**
     * Adds the given components as children of this component.
     *
     * @param components
     *            the components to add
     */
    public void add(Component... components) {
        for (Component component : components) {
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Removes the given child components from this component.
     *
     * @param components
     *            the components to remove
     */
    public void remove(Component... components) {
        for (Component component : components) {
            getElement().removeChild(component.getElement());
        }
    }

    /**
     * Removes all contents from this component, this includes child components,
     * text contents as well as child elements that have been added directly to
     * {@link #getElement()}.
     */
    public void removeAll() {
        getElement().removeAllChildren();
    }

    /**
     * Sets the given string as the content of this component. This removes all
     * child components as well as child elements that have been added directly
     * to {@link #getElement()}.
     *
     * @param text
     *            the text content to set
     */
    public void setText(String text) {
        getElement().setTextContent(text);
    }

    /**
     * Gets the text content of this component. This method only considers text
     * set using {@link #setText(String)} and text nodes directly added to
     * {@link #getElement()}. It does not consider the text contents of any
     * child components or elements.
     *
     * @return the text content of this component, not <code>null</code>
     */
    public String getText() {
        return getElement().getOwnTextContent();
    }
}
