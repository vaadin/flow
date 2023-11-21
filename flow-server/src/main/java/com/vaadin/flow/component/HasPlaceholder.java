package com.vaadin.flow.component;

/**
 * A component which supports a placeholder.
 * <p>
 * A placeholder is a text that should be displayed in the input element,
 * when the user has not entered a value.
 * <p>
 * The default implementations sets the <code>placeholder</code> property for this element.
 * Override all methods in this interface if the placeholder
 * should be set in some other way.
 */
public interface HasPlaceholder extends HasElement {
    /**
     * Sets the placeholder text that should be displayed in the input element,
     * when the user has not entered a value
     *
     * @param placeholder the placeholder text, may be null.
     */
    default void setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
    }

    /**
     * The placeholder text that should be displayed in the input element, when
     * the user has not entered a value
     *
     * @return the {@code placeholder} property from the web component. May be
     * null if not yet set.
     */
    default String getPlaceholder() {
        return getElement().getProperty("placeholder");
    }
}
