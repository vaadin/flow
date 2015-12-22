package com.vaadin.elements.core.grid.selection;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.elements.core.grid.AbstractSelectionModel;

/**
 * A default implementation for a {@link SelectionModel.None}
 */
public class NoSelectionModel extends AbstractSelectionModel
        implements SelectionModel.None {

    @Override
    public boolean isSelected(final Object itemId) {
        return false;
    }

    @Override
    public Collection<Object> getSelectedRows() {
        return Collections.emptyList();
    }

    /**
     * Semantically resets the selection model.
     * <p>
     * Effectively a no-op.
     */
    @Override
    public void reset() {
        // NOOP
    }
}