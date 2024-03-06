/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.reactive;

/**
 * Listens to invalidate events fired by a computation.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface InvalidateListener {
    /**
     * Invoked when an invalidate event is fired.
     *
     * @param event
     *            the invalidate event
     */
    void onInvalidate(InvalidateEvent event);
}
