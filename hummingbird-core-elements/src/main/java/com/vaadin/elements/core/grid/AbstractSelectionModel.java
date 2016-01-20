package com.vaadin.elements.core.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import com.vaadin.data.DataGenerator;
import com.vaadin.data.Item;
import com.vaadin.elements.core.grid.selection.SelectionModel;
import com.vaadin.event.SelectionEvent;
import com.vaadin.event.SelectionEvent.SelectionListener;
import com.vaadin.event.SelectionEvent.SelectionNotifier;
import com.vaadin.shared.ui.grid.GridState;

import elemental.json.JsonObject;

/**
 * A base class for SelectionModels that contains some of the logic that is
 * reusable.
 */
public abstract class AbstractSelectionModel extends AbstractGridExtension
        implements SelectionModel, DataGenerator {
    protected final LinkedHashSet<Object> selection = new LinkedHashSet<Object>();

    @Override
    public boolean isSelected(final Object itemId) {
        return selection.contains(itemId);
    }

    @Override
    public Collection<Object> getSelectedRows() {
        return new ArrayList<Object>(selection);
    }

    @Override
    public void setGrid(final Grid grid) {
        if (grid != null) {
            extend(grid);
        }
    }

    /**
     * Sanity check for existence of item id.
     *
     * @param itemId
     *            item id to be selected / deselected
     *
     * @throws IllegalArgumentException
     *             if item Id doesn't exist in the container of Grid
     */
    protected void checkItemIdExists(Object itemId)
            throws IllegalArgumentException {
        if (!getParent().getContainerDataSource().containsId(itemId)) {
            throw new IllegalArgumentException("Given item id (" + itemId
                    + ") does not exist in the container");
        }
    }

    /**
     * Sanity check for existence of item ids in given collection.
     *
     * @param itemIds
     *            item id collection to be selected / deselected
     *
     * @throws IllegalArgumentException
     *             if at least one item id doesn't exist in the container of
     *             Grid
     */
    protected void checkItemIdsExist(Collection<?> itemIds)
            throws IllegalArgumentException {
        for (Object itemId : itemIds) {
            checkItemIdExists(itemId);
        }
    }

    /**
     * Fires a {@link SelectionEvent} to all the {@link SelectionListener
     * SelectionListeners} currently added to the Grid in which this
     * SelectionModel is.
     * <p>
     * Note that this is only a helper method, and routes the call all the way
     * to Grid. A {@link SelectionModel} is not a {@link SelectionNotifier}
     *
     * @param oldSelection
     *            the complete {@link Collection} of the itemIds that were
     *            selected <em>before</em> this event happened
     * @param newSelection
     *            the complete {@link Collection} of the itemIds that are
     *            selected <em>after</em> this event happened
     */
    protected void fireSelectionEvent(final Collection<Object> oldSelection,
            final Collection<Object> newSelection) {
        getParent().fireSelectionEvent(oldSelection, newSelection);
    }

    @Override
    public void generateData(Object itemId, Item item, JsonObject rowData) {
        if (isSelected(itemId)) {
            rowData.put(GridState.JSONKEY_SELECTED, true);
        }
    }

    // @Override
    // protected Object getItemId(String rowKey) {
    // return rowKey != null ? super.getItemId(rowKey) : null;
    // }
}