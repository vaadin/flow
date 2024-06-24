/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import java.util.Set;

import com.vaadin.flow.internal.StateNode;

/**
 * List of synchronized property events for an element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SynchronizedPropertyEventsList
        extends SerializableNodeList<String> {

    private static class EventsSetView extends NodeList.SetView<String> {

        private EventsSetView(SynchronizedPropertyEventsList list) {
            super(list);
        }

        @Override
        protected void validate(String item) {
            if (item == null) {
                throw new IllegalArgumentException("Event name cannot be null");
            }
        }

    }

    /**
     * Creates a new synchronized property events list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public SynchronizedPropertyEventsList(StateNode node) {
        super(node);
    }

    /**
     * Creates a view into this list.
     *
     * @return a view into this list
     */
    public Set<String> getSynchronizedPropertyEvents() {
        return new EventsSetView(this);
    }

}
