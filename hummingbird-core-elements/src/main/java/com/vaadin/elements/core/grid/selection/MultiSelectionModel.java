package com.vaadin.elements.core.grid.selection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.vaadin.data.Container.Indexed;
import com.vaadin.elements.core.grid.AbstractSelectionModel;
import com.vaadin.shared.ui.grid.selection.MultiSelectionModelState;
import com.vaadin.ui.AbstractClientConnector;

/**
 * A default implementation of a {@link SelectionModel.Multi}
 */
public class MultiSelectionModel extends AbstractSelectionModel
        implements SelectionModel.Multi {

    /**
     * The default selection size limit.
     *
     * @see #setSelectionLimit(int)
     */
    public static final int DEFAULT_MAX_SELECTIONS = 1000;

    private int selectionLimit = DEFAULT_MAX_SELECTIONS;

    @Override
    protected void extend(AbstractClientConnector target) {
        super.extend(target);
        // registerRpc(new MultiSelectionModelServerRpc() {
        //
        // @Override
        // public void select(List<String> rowKeys) {
        // List<Object> items = new ArrayList<Object>();
        // for (String rowKey : rowKeys) {
        // items.add(getItemId(rowKey));
        // }
        // MultiSelectionModel.this.select(items, false);
        // }
        //
        // @Override
        // public void deselect(List<String> rowKeys) {
        // List<Object> items = new ArrayList<Object>();
        // for (String rowKey : rowKeys) {
        // items.add(getItemId(rowKey));
        // }
        // MultiSelectionModel.this.deselect(items, false);
        // }
        //
        // @Override
        // public void selectAll() {
        // MultiSelectionModel.this.selectAll(false);
        // }
        //
        // @Override
        // public void deselectAll() {
        // MultiSelectionModel.this.deselectAll(false);
        // }
        // });
    }

    @Override
    public boolean select(final Object... itemIds)
            throws IllegalArgumentException {
        if (itemIds != null) {
            // select will fire the event
            return select(Arrays.asList(itemIds));
        } else {
            throw new IllegalArgumentException(
                    "Vararg array of itemIds may not be null");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * All items might not be selected if the limit set using
     * {@link #setSelectionLimit(int)} is exceeded.
     */
    @Override
    public boolean select(final Collection<?> itemIds)
            throws IllegalArgumentException {
        return select(itemIds, true);
    }

    protected boolean select(final Collection<?> itemIds, boolean refresh) {
        if (itemIds == null) {
            throw new IllegalArgumentException("itemIds may not be null");
        }

        // Sanity check
        checkItemIdsExist(itemIds);

        final boolean selectionWillChange = !selection.containsAll(itemIds)
                && selection.size() < selectionLimit;
        if (selectionWillChange) {
            final HashSet<Object> oldSelection = new HashSet<Object>(
                    selection);
            if (selection.size() + itemIds.size() >= selectionLimit) {
                // Add one at a time if there's a risk of overflow
                Iterator<?> iterator = itemIds.iterator();
                while (iterator.hasNext()
                        && selection.size() < selectionLimit) {
                    selection.add(iterator.next());
                }
            } else {
                selection.addAll(itemIds);
            }
            fireSelectionEvent(oldSelection, selection);
        }

        updateAllSelectedState();

        if (refresh) {
            for (Object itemId : itemIds) {
                refreshRow(itemId);
            }
        }

        return selectionWillChange;
    }

    /**
     * Sets the maximum number of rows that can be selected at once. This is
     * a mechanism to prevent exhausting server memory in situations where
     * users select lots of rows. If the limit is reached, newly selected
     * rows will not become recorded.
     * <p>
     * Old selections are not discarded if the current number of selected
     * row exceeds the new limit.
     * <p>
     * The default limit is {@value #DEFAULT_MAX_SELECTIONS} rows.
     *
     * @param selectionLimit
     *            the non-negative selection limit to set
     * @throws IllegalArgumentException
     *             if the limit is negative
     */
    public void setSelectionLimit(int selectionLimit) {
        if (selectionLimit < 0) {
            throw new IllegalArgumentException(
                    "The selection limit must be non-negative");
        }
        this.selectionLimit = selectionLimit;
    }

    /**
     * Gets the selection limit.
     *
     * @see #setSelectionLimit(int)
     *
     * @return the selection limit
     */
    public int getSelectionLimit() {
        return selectionLimit;
    }

    @Override
    public boolean deselect(final Object... itemIds)
            throws IllegalArgumentException {
        if (itemIds != null) {
            // deselect will fire the event
            return deselect(Arrays.asList(itemIds));
        } else {
            throw new IllegalArgumentException(
                    "Vararg array of itemIds may not be null");
        }
    }

    @Override
    public boolean deselect(final Collection<?> itemIds)
            throws IllegalArgumentException {
        return deselect(itemIds, true);
    }

    protected boolean deselect(final Collection<?> itemIds,
            boolean refresh) {
        if (itemIds == null) {
            throw new IllegalArgumentException("itemIds may not be null");
        }

        final boolean hasCommonElements = !Collections.disjoint(itemIds,
                selection);
        if (hasCommonElements) {
            final HashSet<Object> oldSelection = new HashSet<Object>(
                    selection);
            selection.removeAll(itemIds);
            fireSelectionEvent(oldSelection, selection);
        }

        updateAllSelectedState();

        if (refresh) {
            for (Object itemId : itemIds) {
                refreshRow(itemId);
            }
        }

        return hasCommonElements;
    }

    @Override
    public boolean selectAll() {
        return selectAll(true);
    }

    protected boolean selectAll(boolean refresh) {
        // select will fire the event
        final Indexed container = getParent().getContainerDataSource();
        if (container != null) {
            return select(container.getItemIds(), refresh);
        } else if (selection.isEmpty()) {
            return false;
        } else {
            /*
             * this should never happen (no container but has a selection),
             * but I guess the only theoretically correct course of
             * action...
             */
            return deselectAll(false);
        }
    }

    @Override
    public boolean deselectAll() {
        return deselectAll(true);
    }

    protected boolean deselectAll(boolean refresh) {
        // deselect will fire the event
        return deselect(getSelectedRows(), refresh);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned Collection is in <strong>order of selection</strong>
     * &ndash; the item that was first selected will be first in the
     * collection, and so on. Should an item have been selected twice
     * without being deselected in between, it will have remained in its
     * original position.
     */
    @Override
    public Collection<Object> getSelectedRows() {
        // overridden only for JavaDoc
        return super.getSelectedRows();
    }

    /**
     * Resets the selection model.
     * <p>
     * Equivalent to calling {@link #deselectAll()}
     */
    @Override
    public void reset() {
        deselectAll();
    }

    @Override
    public boolean setSelected(Collection<?> itemIds)
            throws IllegalArgumentException {
        if (itemIds == null) {
            throw new IllegalArgumentException("itemIds may not be null");
        }

        checkItemIdsExist(itemIds);

        boolean changed = false;
        Set<Object> selectedRows = new HashSet<Object>(itemIds);
        final Collection<Object> oldSelection = getSelectedRows();
        Set<Object> added = new HashSet();
        added.addAll(selectedRows);
        added.removeAll(selection);

        if (!added.isEmpty()) {
            changed = true;
            selection.addAll(added);
        }

        Set<Object> removed = new HashSet();
        removed.addAll(selection);
        removed.removeAll(selectedRows);
        if (!removed.isEmpty()) {
            changed = true;
            selection.removeAll(removed);
        }

        if (changed) {
            fireSelectionEvent(oldSelection, selection);
        }

        updateAllSelectedState();

        return changed;
    }

    @Override
    public boolean setSelected(Object... itemIds)
            throws IllegalArgumentException {
        if (itemIds != null) {
            return setSelected(Arrays.asList(itemIds));
        } else {
            throw new IllegalArgumentException(
                    "Vararg array of itemIds may not be null");
        }
    }

    private void updateAllSelectedState() {
        if (getState().allSelected != selection.size() >= selectionLimit) {
            getState().allSelected = selection.size() >= selectionLimit;
        }
    }

    protected MultiSelectionModelState getState() {
        // return (MultiSelectionModelState) super.getState();
        return null;
    }
}