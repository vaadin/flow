/*
 * Copyright (C) 2000-2026 Vaadin Ltd
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
 * <p>
 * Root element should be a web component with a structure that supports the
 * 'label' property:
 *
 * <pre>{@code
 *     <field-with-label>
 *         <shadow-root>
 *             <input type="checkbox" id="input"/>
 *             <label for="input">${label}</label>
 *         </shadow-root>
 *     </field-with-label>
 * }</pre>
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
