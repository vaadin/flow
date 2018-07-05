/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
