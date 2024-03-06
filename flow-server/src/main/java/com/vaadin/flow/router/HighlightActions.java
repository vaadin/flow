/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasStyle;

/**
 * A set of predefined {@link HighlightAction}s.
 *
 * @since 1.0
 */
public final class HighlightActions {

    private HighlightActions() {
    }

    /**
     * An action which toggles {@code className} class on the target based on
     * its highlight state.
     *
     * @param <C>
     *            the target type
     * @param className
     *            the class name to toggle
     * @return the highlight action
     */
    public static <C extends HasStyle> HighlightAction<C> toggleClassName(
            String className) {
        return (component, highlight) -> component.getClassNames()
                .set(className, highlight);
    }

    /**
     * An action which toggles {@code theme} on the target based on its
     * highlight state.
     *
     * @param <C>
     *            the target type
     * @param theme
     *            the theme to toggle
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> toggleTheme(
            String theme) {
        return (component, highlight) -> component.getElement().getThemeList()
                .set(theme, highlight);
    }

    /**
     * An action which toggles the target's {@code attribute} based on its
     * highlight state.
     *
     * @param <C>
     *            the target type
     * @param attribute
     *            the attribute to toggle
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> toggleAttribute(
            String attribute) {
        return (component, highlight) -> component.getElement()
                .setAttribute(attribute, highlight);
    }

    /**
     * An action which does nothing, regardless of the highlight state.
     *
     * @param <C>
     *            the target type
     * @return the highlight action
     */
    public static <C extends HasElement> HighlightAction<C> none() {
        return (component, highlight) -> {
            // Do nothing.
        };
    }
}
