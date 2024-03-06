/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.event;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortOrder;
import com.vaadin.flow.shared.Registration;

/**
 * Event describing a change in sorting of a {@link DataProvider}. Fired by
 * {@link SortNotifier SortNotifiers}.
 *
 * @see SortOrder
 * @param <T>
 *            the event source type
 * @param <S>
 *            the type of the sorting information
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SortEvent<T extends Component, S extends SortOrder<?>>
        extends ComponentEvent<T> {

    private final List<S> sortOrder;

    /**
     * Creates a new sort order change event with a sort order list.
     *
     * @param source
     *            the component from which the event originates
     * @param sortOrder
     *            the new sort order list
     * @param fromClient
     *            <code>true</code> if event is a result of user interaction,
     *            <code>false</code> if from API call
     */
    public SortEvent(T source, List<S> sortOrder, boolean fromClient) {
        super(source, fromClient);
        this.sortOrder = sortOrder;
    }

    /**
     * Gets the sort order list.
     *
     * @return the sort order list
     */
    public List<S> getSortOrder() {
        return sortOrder;
    }

    /**
     * The interface for adding and removing listeners for {@link SortEvent
     * SortEvents}.
     *
     * @param <T>
     *            the event source type
     * @param <S>
     *            the type of the sorting information
     */
    @FunctionalInterface
    public interface SortNotifier<T extends Component, S extends SortOrder<?>>
            extends Serializable {

        /**
         * Adds a sort order change listener that gets notified when the sort
         * order changes.
         *
         * @param listener
         *            the sort order change listener to add
         * @return a registration object for removing the listener
         */
        Registration addSortListener(
                ComponentEventListener<SortEvent<T, S>> listener);
    }
}
