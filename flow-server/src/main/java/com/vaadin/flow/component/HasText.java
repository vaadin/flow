/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

/**
 * A component that supports text content.
 * <p>
 * The default implementations set the text as text content of
 * {@link #getElement()}. Override all methods in this interface if the text
 * should be added to some other element.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface HasText extends HasElement {

    /**
     * Sets the given string as the content of this component. This removes any
     * existing child components and child elements. To mix text and child
     * components in a component that also supports child components, use
     * {@link HasComponents#add(Component...)} with the {@link Text} component
     * for the textual parts.
     *
     * @param text
     *            the text content to set
     */
    default void setText(String text) {
        getElement().setText(text);
    }

    /**
     * Gets the text content of this component. This method only considers the
     * text of the actual component. The text contents of any child components
     * or elements are not considered.
     *
     * @return the text content of this component, not <code>null</code>
     */
    default String getText() {
        return getElement().getText();
    }
}
