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

import com.vaadin.flow.dom.ElementConstants;

/**
 * A generic interface for components and other user interface objects that may
 * have an aria-label property to set the accessible name of the component.
 * <p>
 * The default implementations set the aria-label of the component to the given
 * {@link #getElement()}. Override all methods in this interface if the
 * aria-label should be added to some other element.
 * <p>
 * The purpose of aria-label is to provide the user with a recognizable name of
 * the component. If the label text is visible on screen, aria-label should not
 * be used. There may be instances where the name of an element cannot be
 * determined programmatically from the content of the element, and there are
 * cases where providing a visible label is not the desired user experience. In
 * the cases where a visible label or visible tooltip is undesirable, aria-label
 * may be used to set the accessible name of the component.
 * <p>
 * See: https://www.w3.org/TR/wai-aria/#aria-label
 * <p>
 * Note: The aria-label property is not valid on every component, see
 * https://www.w3.org/TR/using-aria/#label-support for more details.
 *
 * @author Vaadin Ltd
 * @since
 */
public interface HasAriaLabel extends HasElement {
    /**
     * Set the aria-label of the component to the given text.
     *
     * @param ariaLabel
     *            the aria-label text to set or {@code null} to clear
     */
    default void setAriaLabel(String ariaLabel) {
        if (ariaLabel != null) {
            getElement().setAttribute(ElementConstants.ARIA_LABEL_PROPERTY_NAME,
                    ariaLabel);
        } else {
            getElement()
                    .removeAttribute(ElementConstants.ARIA_LABEL_PROPERTY_NAME);
        }
    }

    /**
     * Gets the aria-label of the component.
     *
     * @return an optional aria-label of the component if no aria-label has been
     *         set
     */
    default Optional<String> getAriaLabel() {
        return Optional.ofNullable(getElement()
                .getAttribute(ElementConstants.ARIA_LABEL_PROPERTY_NAME));
    }
}
