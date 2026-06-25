/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.signals.impl;

/**
 * A listener that is expected to only be invoked the next time some event
 * occurs but not for subsequent events. The listener can optionally request
 * that it retained also for the following event.
 */
@FunctionalInterface
public interface TransientListener {
    /**
     * Invoked when the next event occurs. The return value indicates whether
     * the listener should be retained.
     *
     * @return <code>true</code> to invoke the listener also for the next event,
     *         <code>false</code> to stop invoking the listener
     */
    boolean invoke();
}
