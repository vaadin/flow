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

/**
 * A set of predefined {@link HighlightCondition}s.
 *
 * @since 1.0
 */
public final class HighlightConditions {

    private HighlightConditions() {
    }

    /**
     * Highlight if the navigation path is the same as the target
     * {@link RouterLink}.
     *
     * @return the highlight condition
     */
    public static HighlightCondition<RouterLink> sameLocation() {
        return (link, event) -> event.getLocation().getPath()
                .equals(link.getHref());
    }

    /**
     * Highlight if the navigation path starts with the target
     * {@link RouterLink} path.
     *
     * @return the highlight condition
     */
    public static HighlightCondition<RouterLink> locationPrefix() {
        return (link, event) -> link.getHref().isEmpty()
                ? event.getLocation().getPath().isEmpty()
                : event.getLocation().getPath().startsWith(link.getHref());
    }

    /**
     * Highlight if the navigation path starts with {@code prefix}.
     *
     * @param <C>
     *            the target type
     * @param prefix
     *            the prefix to match on the location path
     * @return the highlight condition
     */
    public static <C extends HasElement> HighlightCondition<C> locationPrefix(
            String prefix) {
        return (component, event) -> event.getLocation().getPath()
                .startsWith(prefix);
    }

    /**
     * Always highlight.
     *
     * @param <C>
     *            the target type
     * @return an always true highlight condition
     */
    public static <C extends HasElement> HighlightCondition<C> always() {
        return (component, event) -> true;
    }

    /**
     * Never highlight.
     *
     * @param <C>
     *            the target type
     * @return an always false highlight condition
     */
    public static <C extends HasElement> HighlightCondition<C> never() {
        return (component, event) -> false;
    }
}
