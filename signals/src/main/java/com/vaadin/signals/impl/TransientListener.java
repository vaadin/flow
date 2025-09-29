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
     * @param immdediate
     *            <code>true</code> if the listener is invoked immediately when
     *            it is added, <code>false</code> if the event occurred after
     *            the listener was added
     *
     * @return <code>true</code> to invoke the listener also for the next event,
     *         <code>false</code> to stop invoking the listener
     */
    boolean invoke(boolean immdediate);
}
