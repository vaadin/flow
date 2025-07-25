/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.data.provider;

import java.util.EventObject;
import java.util.Objects;

import com.vaadin.flow.server.Command;

/**
 * An event fired when the data of a {@code DataProvider} changes.
 *
 * @see DataProviderListener
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the data type
 */
public class DataChangeEvent<T> extends EventObject {

    private Command unregisterListenerCommand = null;

    /**
     * An event fired when a single item of a {@code DataProvider} has been
     * updated.
     *
     * @param <T>
     *            the data type
     */
    public static class DataRefreshEvent<T> extends DataChangeEvent<T> {

        private final T item;
        private boolean refreshChildren;

        /**
         * Creates a new data refresh event originating from the given data
         * provider.
         *
         * @param source
         *            the data provider, not null
         * @param item
         *            the updated item, not null
         */
        public DataRefreshEvent(DataProvider<T, ?> source, T item) {
            this(source, item, false);
        }

        /**
         * Creates a new data refresh event originating from the given data
         * provider.
         *
         * @param source
         *            the data provider, not null
         * @param item
         *            the updated item, not null
         * @param refreshChildren
         *            whether, in hierarchical providers, subelements should be
         *            refreshed as well
         */
        public DataRefreshEvent(DataProvider<T, ?> source, T item,
                boolean refreshChildren) {
            super(source);
            Objects.requireNonNull(item, "Refreshed item can't be null");
            this.item = item;
            this.refreshChildren = refreshChildren;
        }

        /**
         * Gets the refreshed item.
         *
         * @return the refreshed item
         */
        public T getItem() {
            return item;
        }

        /**
         * Gets the a boolean whether the refresh is supposed to be
         * refreshChildren (in hierarchical data providers).
         *
         * @return whether, in hierarchical providers, subelements should be
         *         refreshed as well
         * @deprecated since 24.9 and will be removed in Vaadin 25.
         */
        @Deprecated(since = "24.9", forRemoval = true)
        public boolean isRefreshChildren() {
            return refreshChildren;
        }
    }

    /**
     * Creates a new {@code DataChangeEvent} event originating from the given
     * data provider.
     *
     * @param source
     *            the data provider, not null
     */
    public DataChangeEvent(DataProvider<T, ?> source) {
        super(source);
    }

    @Override
    public DataProvider<T, ?> getSource() {
        return (DataProvider<T, ?>) super.getSource();
    }

    /**
     * Sets the command which is executed to unregister the listener.
     * <p>
     * For internal use.
     *
     * @param unregisterListenerCommand
     *            the unregister command
     */
    void setUnregisterListenerCommand(Command unregisterListenerCommand) {
        this.unregisterListenerCommand = unregisterListenerCommand;
    }

    /**
     * Unregisters the event listener currently being invoked.
     * <p>
     * This method can only be called from within an event listener otherwise it
     * throws an {@link IllegalStateException}. Calling it will remove the
     * current event listener so no further events are passed to it.
     *
     * @throws IllegalStateException
     *             if the method is called outside of the event listener.
     */
    public void unregisterListener() throws IllegalStateException {
        if (unregisterListenerCommand == null) {
            throw new IllegalStateException(
                    "unregisterListener can only be called inside the event listener");
        }
        unregisterListenerCommand.execute();
    }
}
