package com.vaadin.elements.core.grid;

import java.io.Serializable;

import com.vaadin.data.Item;

/**
 * A data class which contains information which identifies a row in a
 * {@link Grid}.
 * <p>
 * Since this class follows the <code>Flyweight</code>-pattern any instance of
 * this object is subject to change without the user knowing it and so should
 * not be stored anywhere outside of the method providing these instances.
 */
public class RowReference implements Serializable {
    private final Grid grid;

    private Object itemId;

    /**
     * Creates a new row reference for the given grid.
     *
     * @param grid
     *            the grid that the row belongs to
     */
    public RowReference(Grid grid) {
        this.grid = grid;
    }

    /**
     * Sets the identifying information for this row
     *
     * @param itemId
     *            the item id of the row
     */
    public void set(Object itemId) {
        this.itemId = itemId;
    }

    /**
     * Gets the grid that contains the referenced row.
     *
     * @return the grid that contains referenced row
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Gets the item id of the row.
     *
     * @return the item id of the row
     */
    public Object getItemId() {
        return itemId;
    }

    /**
     * Gets the item for the row.
     *
     * @return the item for the row
     */
    public Item getItem() {
        return grid.getContainerDataSource().getItem(itemId);
    }
}