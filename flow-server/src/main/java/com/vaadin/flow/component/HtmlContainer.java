/*
 * Copyright 2000-2018 Vaadin Ltd.
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
 * Base class for a {@link Component} that represents a single built-in HTML
 * element that can contain child components or text.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class HtmlContainer extends HtmlComponent
        implements HasComponents, HasText {
    /**
     * Creates an empty component with the element determined by the {@link Tag}
     * annotation of a sub class.
     */
    protected HtmlContainer() {
        super();
    }

    /**
     * Creates a component with the given child components. The element is
     * determined by the {@link Tag} annotation of a sub class.
     *
     * @param components
     *            the child components
     */
    protected HtmlContainer(Component... components) {
        add(components);
    }

    /**
     * Creates a new empty component with a new element with the given tag name.
     *
     * @param tagName
     *            the tag name of the element to use for this component, not
     *            <code>null</code>
     */
    public HtmlContainer(String tagName) {
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
    public HtmlContainer(String tagName, Component... components) {
        super(tagName);
        add(components);
    }

}
