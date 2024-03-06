/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.template.internal;

import com.vaadin.flow.dom.Element;

/**
 * Defines the strategy to set the template attribute value to the server side
 * element.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
interface ElementInitializationStrategy {

    /**
     * Initializes the {@code element} with template attribute {@code name} and
     * its {@code value}.
     *
     * @param element
     *            the element to initialize
     * @param name
     *            the template attribute name
     * @param value
     *            the attribute value
     */
    void initialize(Element element, String name, String value);
}
