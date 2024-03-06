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
 * Listens to changes to a reactive value.
 *
 * @see ReactiveValue#addReactiveValueChangeListener(ReactiveValueChangeListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface ReactiveValueChangeListener {
    /**
     * Invoked when a reactive value has changed.
     *
     * @param event
     *            the change event
     */
    void onValueChange(ReactiveValueChangeEvent event);
}
