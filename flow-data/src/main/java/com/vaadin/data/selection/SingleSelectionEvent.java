package com.vaadin.data.selection;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.vaadin.data.AbstractListing;
import com.vaadin.ui.common.HasValue;

public class SingleSelectionEvent<L extends AbstractListing<T>, T>
        extends HasValue.ValueChangeEvent<L, T> implements SelectionEvent<T> {

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
    public SingleSelectionEvent(L listing, SingleSelect<L, T> source,
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
    public L getSource() {
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
