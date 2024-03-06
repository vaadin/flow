/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.selection;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;

/**
 * Fired when the selection changes in a listing component.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <C>
 *            the selection component type
 * @param <T>
 *            the type of the selected item
 */
public class SingleSelectionEvent<C extends Component, T> extends
        ComponentValueChangeEvent<C, T> implements SelectionEvent<C, T> {

    /**
     * Creates a new selection change event in a component.
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
    public SingleSelectionEvent(C listing, SingleSelect<C, T> source,
            T oldSelection, boolean userOriginated) {
        super(listing, source, oldSelection, userOriginated);
    }

    /**
     * Returns an optional of the item that was selected, or an empty optional
     * if a previously selected item was deselected.
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
    public C getSource() {
        return super.getSource();
    }

    @Override
    public Optional<T> getFirstSelectedItem() {
        return getSelectedItem();
    }

    @Override
    public Set<T> getAllSelectedItems() {
        Optional<T> selectedItem = getSelectedItem();
        if (selectedItem.isPresent()) {
            return Collections.singleton(selectedItem.get());
        } else {
            return Collections.emptySet();
        }
    }
}
