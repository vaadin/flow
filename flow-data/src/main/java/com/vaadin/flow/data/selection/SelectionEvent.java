/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.selection;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;

/**
 * A selection event that unifies the way to access to selection event for multi
 * selection and single selection components (in case when only one selected
 * item is required).
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the data type of the selection model
 * @param <C>
 *            the component type
 */
public interface SelectionEvent<C extends Component, T> extends Serializable {

    /**
     * Get first selected data item.
     * <p>
     * This is the same as {@link SingleSelectionEvent#getSelectedItem()} in
     * case of single selection.
     *
     * @return the first selected item.
     */
    Optional<T> getFirstSelectedItem();

    /**
     * Gets all the currently selected items.
     * <p>
     * This method applies more to multiselection - for single select it returns
     * either an empty set or a set containing the only selected item.
     *
     * @return return all the selected items, if any, never {@code null}
     */
    Set<T> getAllSelectedItems();

    /**
     * The component on which the Event initially occurred.
     *
     * @return The component on which the Event initially occurred.
     */
    C getSource();

    /**
     * Checks if this event originated from the client side.
     *
     * @return <code>true</code> if the event originated from the client side,
     *         <code>false</code> otherwise
     */
    boolean isFromClient();
}
