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

import java.util.Optional;

import com.vaadin.flow.dom.Element;

/**
 * Base class for a {@link Component} that represents a single built-in HTML
 * element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class HtmlComponent extends Component implements HasSize, HasStyle {
    private static final PropertyDescriptor<String, Optional<String>> titleDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("title", "");

    /**
     * Creates a component with the element type based on the {@link Tag}
     * annotation of the sub class.
     */
    protected HtmlComponent() {
        // Creates element based on @Tag
        super();
    }

    /**
     * Creates a component with a new element with the given tag name.
     *
     * @param tagName
     *            the tag name of the element to use for this component, not
     *            <code>null</code>
     */
    public HtmlComponent(String tagName) {
        super(new Element(tagName));
    }

    /**
     * Sets the title of this component. Browsers typically use the title to
     * show a tooltip when hovering an element or any descendant without a title
     * value of its own.
     *
     * @param title
     *            the title value to set, or <code>""</code> to remove any
     *            previously set title
     */
    public void setTitle(String title) {
        set(titleDescriptor, title);
    }

    /**
     * Gets the title of this component.
     *
     * @see #setTitle(String)
     *
     * @return an optional title, or an empty optional if no title has been set
     *
     */
    public Optional<String> getTitle() {
        return get(titleDescriptor);
    }

}
