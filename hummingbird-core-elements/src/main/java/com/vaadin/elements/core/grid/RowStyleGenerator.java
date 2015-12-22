package com.vaadin.elements.core.grid;

import java.io.Serializable;

/**
 * A callback interface for generating custom style names for Grid rows.
 *
 * @see Grid#setRowStyleGenerator(RowStyleGenerator)
 */
public interface RowStyleGenerator extends Serializable {

    /**
     * Called by Grid to generate a style name for a row.
     *
     * @param row
     *            the row to generate a style for
     * @return the style name to add to this row, or {@code null} to not set
     *         any style
     */
    public String getStyle(RowReference row);
}