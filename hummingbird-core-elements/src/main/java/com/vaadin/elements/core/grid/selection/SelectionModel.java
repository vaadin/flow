package com.vaadin.elements.core.grid.selection;

import java.io.Serializable;
import java.util.Collection;

import com.vaadin.elements.core.grid.AbstractGridExtension;
import com.vaadin.elements.core.grid.Grid;

/**
 * The server-side interface that controls Grid's selection state.
 * SelectionModel should extend {@link AbstractGridExtension}.
 */
public interface SelectionModel extends Serializable {
    /**
     * Checks whether an item is selected or not.
     *
     * @param itemId
     *            the item id to check for
     * @return <code>true</code> iff the item is selected
     */
    boolean isSelected(Object itemId);

    /**
     * Returns a collection of all the currently selected itemIds.
     *
     * @return a collection of all the currently selected itemIds
     */
    Collection<Object> getSelectedRows();

    /**
     * Injects the current {@link Grid} instance into the SelectionModel.
     * This method should usually call the extend method of
     * {@link AbstractExtension}.
     * <p>
     * <em>Note:</em> This method should not be called manually.
     *
     * @param grid
     *            the Grid in which the SelectionModel currently is, or
     *            <code>null</code> when a selection model is being detached
     *            from a Grid.
     */
    void setGrid(Grid grid);

    /**
     * Resets the SelectiomModel to an initial state.
     * <p>
     * Most often this means that the selection state is cleared, but
     * implementations are free to interpret the "initial state" as they
     * wish. Some, for example, may want to keep the first selected item as
     * selected.
     */
    void reset();

    /**
     * A SelectionModel that supports multiple selections to be made.
     * <p>
     * This interface has a contract of having the same behavior, no matter
     * how the selection model is interacted with. In other words, if
     * something is forbidden to do in e.g. the user interface, it must also
     * be forbidden to do in the server-side and client-side APIs.
     */
    public interface Multi extends SelectionModel {

        /**
         * Marks items as selected.
         * <p>
         * This method does not clear any previous selection state, only
         * adds to it.
         *
         * @param itemIds
         *            the itemId(s) to mark as selected
         * @return <code>true</code> if the selection state changed.
         *         <code>false</code> if all the given itemIds already were
         *         selected
         * @throws IllegalArgumentException
         *             if the <code>itemIds</code> varargs array is
         *             <code>null</code> or given itemIds don't exist in the
         *             container of Grid
         * @see #deselect(Object...)
         */
        boolean select(Object... itemIds) throws IllegalArgumentException;

        /**
         * Marks items as selected.
         * <p>
         * This method does not clear any previous selection state, only
         * adds to it.
         *
         * @param itemIds
         *            the itemIds to mark as selected
         * @return <code>true</code> if the selection state changed.
         *         <code>false</code> if all the given itemIds already were
         *         selected
         * @throws IllegalArgumentException
         *             if <code>itemIds</code> is <code>null</code> or given
         *             itemIds don't exist in the container of Grid
         * @see #deselect(Collection)
         */
        boolean select(Collection<?> itemIds)
                throws IllegalArgumentException;

        /**
         * Marks items as deselected.
         *
         * @param itemIds
         *            the itemId(s) to remove from being selected
         * @return <code>true</code> if the selection state changed.
         *         <code>false</code> if none the given itemIds were
         *         selected previously
         * @throws IllegalArgumentException
         *             if the <code>itemIds</code> varargs array is
         *             <code>null</code>
         * @see #select(Object...)
         */
        boolean deselect(Object... itemIds) throws IllegalArgumentException;

        /**
         * Marks items as deselected.
         *
         * @param itemIds
         *            the itemId(s) to remove from being selected
         * @return <code>true</code> if the selection state changed.
         *         <code>false</code> if none the given itemIds were
         *         selected previously
         * @throws IllegalArgumentException
         *             if <code>itemIds</code> is <code>null</code>
         * @see #select(Collection)
         */
        boolean deselect(Collection<?> itemIds)
                throws IllegalArgumentException;

        /**
         * Marks all the items in the current Container as selected
         *
         * @return <code>true</code> iff some items were previously not
         *         selected
         * @see #deselectAll()
         */
        boolean selectAll();

        /**
         * Marks all the items in the current Container as deselected
         *
         * @return <code>true</code> iff some items were previously selected
         * @see #selectAll()
         */
        boolean deselectAll();

        /**
         * Marks items as selected while deselecting all items not in the
         * given Collection.
         *
         * @param itemIds
         *            the itemIds to mark as selected
         * @return <code>true</code> if the selection state changed.
         *         <code>false</code> if all the given itemIds already were
         *         selected
         * @throws IllegalArgumentException
         *             if <code>itemIds</code> is <code>null</code> or given
         *             itemIds don't exist in the container of Grid
         */
        boolean setSelected(Collection<?> itemIds)
                throws IllegalArgumentException;

        /**
         * Marks items as selected while deselecting all items not in the
         * varargs array.
         *
         * @param itemIds
         *            the itemIds to mark as selected
         * @return <code>true</code> if the selection state changed.
         *         <code>false</code> if all the given itemIds already were
         *         selected
         * @throws IllegalArgumentException
         *             if the <code>itemIds</code> varargs array is
         *             <code>null</code> or given itemIds don't exist in the
         *             container of Grid
         */
        boolean setSelected(Object... itemIds)
                throws IllegalArgumentException;
    }

    /**
     * A SelectionModel that supports for only single rows to be selected at
     * a time.
     * <p>
     * This interface has a contract of having the same behavior, no matter
     * how the selection model is interacted with. In other words, if
     * something is forbidden to do in e.g. the user interface, it must also
     * be forbidden to do in the server-side and client-side APIs.
     */
    public interface Single extends SelectionModel {

        /**
         * Marks an item as selected.
         *
         * @param itemId
         *            the itemId to mark as selected; <code>null</code> for
         *            deselect
         * @return <code>true</code> if the selection state changed.
         *         <code>false</code> if the itemId already was selected
         * @throws IllegalStateException
         *             if the selection was illegal. One such reason might
         *             be that the given id was null, indicating a deselect,
         *             but implementation doesn't allow deselecting.
         *             re-selecting something
         * @throws IllegalArgumentException
         *             if given itemId does not exist in the container of
         *             Grid
         */
        boolean select(Object itemId)
                throws IllegalStateException, IllegalArgumentException;

        /**
         * Gets the item id of the currently selected item.
         *
         * @return the item id of the currently selected item, or
         *         <code>null</code> if nothing is selected
         */
        Object getSelectedRow();

        /**
         * Sets whether it's allowed to deselect the selected row through
         * the UI. Deselection is allowed by default.
         *
         * @param deselectAllowed
         *            <code>true</code> if the selected row can be
         *            deselected without selecting another row instead;
         *            otherwise <code>false</code>.
         */
        public void setDeselectAllowed(boolean deselectAllowed);

        /**
         * Sets whether it's allowed to deselect the selected row through
         * the UI.
         *
         * @return <code>true</code> if deselection is allowed; otherwise
         *         <code>false</code>
         */
        public boolean isDeselectAllowed();
    }

    /**
     * A SelectionModel that does not allow for rows to be selected.
     * <p>
     * This interface has a contract of having the same behavior, no matter
     * how the selection model is interacted with. In other words, if the
     * developer is unable to select something programmatically, it is not
     * allowed for the end-user to select anything, either.
     */
    public interface None extends SelectionModel {

        /**
         * {@inheritDoc}
         *
         * @return always <code>false</code>.
         */
        @Override
        public boolean isSelected(Object itemId);

        /**
         * {@inheritDoc}
         *
         * @return always an empty collection.
         */
        @Override
        public Collection<Object> getSelectedRows();
    }
}