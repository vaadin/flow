package com.vaadin.ui;

import com.vaadin.data.Binder;

/**
 * Multiselection model interface for Grid.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the type of items in grid
 */
public interface GridMultiSelectionModel<T>
        extends GridSelectionModel<T>, SelectionModel.Multi<T> {

    /**
     * Gets a wrapper to use this multiselection model as a multiselect in
     * {@link Binder}.
     *
     * @return the multiselect wrapper
     */
    MultiSelect<? extends Grid<T>, T> asMultiSelect();
}
