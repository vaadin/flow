package com.vaadin.ui;

/**
 * The server-side interface that controls Grid's selection state.
 *
 * @param <T>
 *            the grid bean type
 */
public interface GridSelectionModel<T> extends SelectionModel<T> {

    /**
     * Removes this selection model from the grid.
     */
    void remove();

    /**
     * TODO
     *
     * @param item
     */
    void selectFromClient(T item);

    /**
     * TODO
     *
     * @param item
     */
    void deselectFromClient(T item);
}