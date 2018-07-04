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
package com.vaadin.flow.data.selection;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;

/**
 * Fired when the selection changes in a listing component that supports
 * multiple item selection.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <C>
 *            the selection component type
 * @param <T>
 *            the type of the selected item
 */
public class MultiSelectionEvent<C extends Component, T> extends
        ComponentValueChangeEvent<C, Set<T>> implements SelectionEvent<C, T> {

    /**
     * Creates a new multi selection change event in a component.
     *
     * @param listing
     *            the listing component where the event originated
     * @param source
     *            the single select source
     * @param oldSelection
     *            the item that was previously selected
     * @param userOriginated
     *            {@code true} if this event originates from the client,
     *            {@code false} otherwise.
     */
    public MultiSelectionEvent(C listing,
            HasValue<ComponentValueChangeEvent<C, Set<T>>, Set<T>> source,
            Set<T> oldSelection, boolean userOriginated) {
        super(listing, source, oldSelection, userOriginated);
    }

    @Override
    public Optional<T> getFirstSelectedItem() {
        return getValue().stream().findFirst();
    }

    @Override
    public Set<T> getAllSelectedItems() {
        return getValue();
    }

    /**
     * Gets the current selection.
     *
     * @return an unmodifiable set of items selected after the selection was
     *         changed
     */
    @Override
    public Set<T> getValue() {
        return Collections.unmodifiableSet(super.getValue());
    }

    /**
     * Gets the new selection.
     * <p>
     * The result is the current selection of the source listing. So it's always
     * exactly the same as {@link #getValue()}.
     *
     * @see #getValue()
     *
     * @return an unmodifiable set of items selected after the selection was
     *         changed
     */
    public Set<T> getNewSelection() {
        return getValue();
    }

    /**
     * Gets the old selection.
     *
     * @return an unmodifiable set of items selected before the selection was
     *         changed
     */
    public Set<T> getOldSelection() {
        return Collections.unmodifiableSet(getOldValue());
    }

    /**
     * Gets the items that were removed from selection.
     * <p>
     * This is just a convenience method for checking what was previously
     * selected in {@link #getOldSelection()} but not selected anymore in
     * {@link #getNewSelection()}.
     *
     * @return the items that were removed from selection
     */
    public Set<T> getRemovedSelection() {
        Set<T> copy = new LinkedHashSet<>(getOldValue());
        copy.removeAll(getNewSelection());
        return copy;
    }

    /**
     * Gets the items that were added to selection.
     * <p>
     * This is just a convenience method for checking what is new selected in
     * {@link #getNewSelection()} and wasn't selected in
     * {@link #getOldSelection()}.
     *
     * @return the items that were removed from selection
     */
    public Set<T> getAddedSelection() {
        Set<T> copy = new LinkedHashSet<>(getValue());
        copy.removeAll(getOldValue());
        return copy;
    }
}
