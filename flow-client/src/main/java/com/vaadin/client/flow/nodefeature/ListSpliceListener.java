/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.nodefeature;

/**
 * Listener notified when the structure of a node list changes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface ListSpliceListener {
    /**
     * Invoked when the structure of a node list changes.
     *
     * @param event
     *            the list splice event
     */
    void onSplice(ListSpliceEvent event);
}
