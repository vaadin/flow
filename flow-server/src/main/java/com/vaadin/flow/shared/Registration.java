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
package com.vaadin.flow.shared;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Objects;

import com.vaadin.flow.server.Command;

/**
 * A registration object for removing an event listener added to a source.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface Registration extends Serializable {
    /**
     * Removes the associated listener from the event source.
     * <p>
     * The {@code remove} method called after removal does nothing.
     *
     * @see #once(Command)
     */
    void remove();

    /**
     * Creates a registration that will run a command only once. This makes it
     * safe for the registration to be removed multiple times even in cases when
     * the command should be run only once.
     *
     * @param command
     *            the command to run the first time the registration is removed,
     *            not <code>null</code>
     * @return a registration that will invoke the command once, not
     *         <code>null</code>
     * @since
     */
    @GwtIncompatible("Command is not in a package available to GWT")
    static Registration once(Command command) {
        Objects.requireNonNull(command);
        return new Registration() {
            private boolean removed = false;

            @Override
            public void remove() {
                if (!removed) {
                    removed = true;
                    command.execute();
                }
            }
        };
    }

    /**
     * Creates a registration that will remove multiple registrations.
     *
     * @param registrations
     *            the registrations to remove, not <code>null</code>
     * @return a registration that removes all provided registrations, not
     *         <code>null</code>
     * @since
     */
    static Registration combine(Registration... registrations) {
        Objects.requireNonNull(registrations);
        Arrays.asList(registrations).forEach(Objects::requireNonNull);

        return () -> Arrays.asList(registrations).forEach(Registration::remove);
    }

    /**
     * Creates a registration by adding an item to a collection immediately and
     * removing the item from the collection when the registration is removed.
     * <p>
     * Care should be used when using this pattern to iterate over a copy of the
     * collection to avoid {@link ConcurrentModificationException} if a listener
     * or other callback may trigger adding or removing registrations.
     *
     * @param collection
     *            the collection to which the item should be added and removed,
     *            not <code>null</code>
     * @param item
     *            the item to add and remove
     * @return a registration that will remove the item from the collection, not
     *         <code>null</code>
     * @since
     */
    static <T> Registration addAndRemove(Collection<T> collection, T item) {
        collection.add(item);
        return () -> collection.remove(item);
    }

}
