package com.vaadin.ui;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @param <T>
 */
public class GridNoneSelectionModel<T> implements GridSelectionModel<T> {

    @Override
    public Set<T> getSelectedItems() {
        return Collections.emptySet();
    }

    @Override
    public Optional<T> getFirstSelectedItem() {
        return Optional.empty();
    }

    @Override
    public void select(T item) {
    }

    @Override
    public void deselect(T item) {
    }

    @Override
    public void deselectAll() {
    }

    @Override
    public void remove() {
    }

    @Override
    public void selectFromClient(T item) {
        throw new IllegalStateException("Client tried to update selection"
                + " even though selection mode is currently set to NONE.");
    }

    @Override
    public void deselectFromClient(T item) {
        throw new IllegalStateException("Client tried to update selection"
                + " even though selection mode is currently set to NONE.");
    }
}
