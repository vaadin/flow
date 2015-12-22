package com.vaadin.elements.core.grid;

import java.io.Serializable;

import com.vaadin.data.Item;
import com.vaadin.data.Property;

/**
 * A data class which contains information which identifies a cell in a
 * {@link Grid}.
 * <p>
 * Since this class follows the <code>Flyweight</code>-pattern any instance
 * of this object is subject to change without the user knowing it and so
 * should not be stored anywhere outside of the method providing these
 * instances.
 */
public class CellReference implements Serializable {
    private final RowReference rowReference;

    private Object propertyId;

    public CellReference(RowReference rowReference) {
        this.rowReference = rowReference;
    }

    /**
     * Sets the identifying information for this cell
     *
     * @param propertyId
     *            the property id of the column
     */
    public void set(Object propertyId) {
        this.propertyId = propertyId;
    }

    /**
     * Gets the grid that contains the referenced cell.
     *
     * @return the grid that contains referenced cell
     */
    public Grid getGrid() {
        return rowReference.getGrid();
    }

    /**
     * @return the property id of the column
     */
    public Object getPropertyId() {
        return propertyId;
    }

    /**
     * @return the property for the cell
     */
    public Property<?> getProperty() {
        return getItem().getItemProperty(propertyId);
    }

    /**
     * Gets the item id of the row of the cell.
     *
     * @return the item id of the row
     */
    public Object getItemId() {
        return rowReference.getItemId();
    }

    /**
     * Gets the item for the row of the cell.
     *
     * @return the item for the row
     */
    public Item getItem() {
        return rowReference.getItem();
    }

    /**
     * Gets the value of the cell.
     *
     * @return the value of the cell
     */
    public Object getValue() {
        return getProperty().getValue();
    }
}