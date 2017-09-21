package com.vaadin.ui;

import com.vaadin.data.Binder;

/**
 * Single selection model interface for Grid.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the type of items in grid
 */
public interface GridSingleSelectionModel<T>
        extends GridSelectionModel<T>, SelectionModel.Single<T> {

    /**
     * Gets a wrapper to use this single selection model as a single select in
     * {@link Binder}.
     *
     * @return the single select wrapper
     */
    SingleSelect<? extends Grid<T>, T> asSingleSelect();
}
