/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.dom.ElementConstants;

/**
 * A component that supports label definition.
 * <p>
 * The default implementations set the label of the component to the given text
 * for {@link #getElement()}. Override all methods in this interface if the text
 * should be added to some other element.
 *
 *
 * @author Vaadin Ltd
 * @since
 */
public interface HasLabel extends HasElement {
    /**
     * Set the label of the component to the given text.
     *
     * @param label
     *            the label text to set or {@code null} to clear
     */
    default void setLabel(String label) {
        getElement().setProperty(ElementConstants.LABEL_PROPERTY_NAME, label);
    }

    /**
     * Gets the label of the component.
     *
     * @return the label of the component or {@code null} if no label has been
     *         set
     */
    default String getLabel() {
        return getElement().getProperty(ElementConstants.LABEL_PROPERTY_NAME,
                null);
    }
}
