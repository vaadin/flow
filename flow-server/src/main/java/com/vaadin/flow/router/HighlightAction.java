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
 * An action to be performed to set the highlight state of the target.
 *
 * @param <T>
 *            the target type of the highlight action
 * @since 1.0
 */
@FunctionalInterface
public interface HighlightAction<T> extends Serializable {

    /**
     * Performs the highlight action on the target.
     *
     * @param t
     *            the target of the highlight action
     * @param highlight
     *            true if the target should be highlighted, false to clear the
     *            highlight state previously set by this action
     */
    void highlight(T t, boolean highlight);
}
