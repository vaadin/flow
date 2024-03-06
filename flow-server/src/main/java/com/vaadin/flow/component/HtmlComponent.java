/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
