/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
