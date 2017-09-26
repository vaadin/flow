/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.data.selection;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.vaadin.data.AbstractListing;
import com.vaadin.ui.common.HasValue;

/**
 * A selection event that unifies the way to access to selection event for multi
 * selection and single selection components (in case when only one selected
 * item is required).
 *
 * @author Vaadin Ltd
 * @param <L>
 *            the listing component type
 * @param <T>
 *            the data type of the selection model
 */
public class SelectionEvent<L extends AbstractListing<T>, T>
        extends HasValue.ValueChangeEvent<L, T> {

    /**
     * Creates a new selection change event in a component.
     *
     * @param component
     *            the component where the event originated
     * @param source
     *            the single select source
     * @param oldSelection
     *            the item that was previously selected
     * @param userOriginated
     *            {@code true} if this event originates from the client,
     *            {@code false} otherwise.
     */
    public SelectionEvent(L listing, SingleSelect<L, T> source, T oldSelection,
            boolean userOriginated) {
        super(listing, source, oldSelection, userOriginated);
    }

    /**
     * Returns an optional of the item that was selected, or an empty optional
     * if a previously selected item was deselected.
     * <p>
     * The result is the current selection of the source
     * {@link AbstractSingleSelect} object. So it's always exactly the same as
     * optional describing {@link AbstractSingleSelect#getValue()}.
     *
     * @see #getValue()
     *
     * @return the selected item or an empty optional if deselected
     *
     * @see SelectionModel.Single#getSelectedItem()
     */
    public Optional<T> getSelectedItem() {
        return Optional.ofNullable(getValue());
    }

    /**
     * The listing component on which the Event initially occurred.
     *
     * @return The listing component on which the Event initially occurred.
     */
    @Override
    public L getSource() {
        return super.getSource();
    }

    public Optional<T> getFirstSelectedItem() {
        return getSelectedItem();
    }

    public Set<T> getAllSelectedItems() {
        Optional<T> selectedItem = getSelectedItem();
        if (selectedItem.isPresent()) {
            return Collections.singleton(selectedItem.get());
        } else {
            return Collections.emptySet();
        }
    }
}
