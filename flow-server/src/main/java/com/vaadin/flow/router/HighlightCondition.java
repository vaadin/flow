/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;

/**
 * A condition to meet to determine the highlight state of the target.
 *
 * @param <T>
 *            the target type of the highlight condition
 * @since 1.0
 */
@FunctionalInterface
public interface HighlightCondition<T> extends Serializable {

    /**
     * Tests if the target should be highlighted based on the navigation
     * {@code event}.
     *
     * @param t
     *            the target of the highlight condition
     * @param event
     *            the navigation event
     * @return true if the condition is met, false otherwise
     */
    boolean shouldHighlight(T t, AfterNavigationEvent event);
}
