package com.vaadin.elements.core.grid.selection;

import java.util.Collection;
import java.util.Collections;

import com.vaadin.elements.core.grid.AbstractSelectionModel;
import com.vaadin.shared.ui.grid.selection.SingleSelectionModelState;
import com.vaadin.ui.AbstractClientConnector;

/**
 * A default implementation of a {@link SelectionModel.Single}
 */
public class SingleSelectionModel extends AbstractSelectionModel
        implements SelectionModel.Single {

    @Override
    protected void extend(AbstractClientConnector target) {
        super.extend(target);
        // registerRpc(new SingleSelectionModelServerRpc() {
        //
        // @Override
        // public void select(String rowKey) {
        // SingleSelectionModel.this.select(getItemId(rowKey), false);
        // }
        // });
    }

    @Override
    public boolean select(final Object itemId) {
        return select(itemId, true);
    }

    protected boolean select(final Object itemId, boolean refresh) {
        if (itemId == null) {
            return deselect(getSelectedRow());
        }

        checkItemIdExists(itemId);

        final Object selectedRow = getSelectedRow();
        final boolean modified = selection.add(itemId);
        if (modified) {
            final Collection<Object> deselected;
            if (selectedRow != null) {
                deselectInternal(selectedRow, false, true);
                deselected = Collections.singleton(selectedRow);
            } else {
                deselected = Collections.emptySet();
            }

            fireSelectionEvent(deselected, selection);
        }

        if (refresh) {
            refreshRow(itemId);
        }

        return modified;
    }

    private boolean deselect(final Object itemId) {
        return deselectInternal(itemId, true, true);
    }

    private boolean deselectInternal(final Object itemId,
            boolean fireEventIfNeeded, boolean refresh) {
        final boolean modified = selection.remove(itemId);
        if (modified) {
            if (refresh) {
                refreshRow(itemId);
            }
            if (fireEventIfNeeded) {
                fireSelectionEvent(Collections.singleton(itemId),
                        Collections.emptySet());
            }
        }
        return modified;
    }

    @Override
    public Object getSelectedRow() {
        if (selection.isEmpty()) {
            return null;
        } else {
            return selection.iterator().next();
        }
    }

    /**
     * Resets the selection state.
     * <p>
     * If an item is selected, it will become deselected.
     */
    @Override
    public void reset() {
        deselect(getSelectedRow());
    }

    @Override
    public void setDeselectAllowed(boolean deselectAllowed) {
        getState().deselectAllowed = deselectAllowed;
    }

    @Override
    public boolean isDeselectAllowed() {
        return getState().deselectAllowed;
    }

    // @Override
    protected SingleSelectionModelState getState() {
        // return (SingleSelectionModelState) super.getState();
        return null;
    }
}