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
 * Listener that is invoked by {@link Reactive#flush()}.
 *
 * @see Reactive#addFlushListener(FlushListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface FlushListener {
    /**
     * Invoked on {@link Reactive#flush()}.
     */
    void flush();
}
