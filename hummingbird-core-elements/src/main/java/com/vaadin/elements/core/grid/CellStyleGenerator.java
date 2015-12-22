package com.vaadin.elements.core.grid;

import java.io.Serializable;

/**
 * A callback interface for generating custom style names for Grid cells.
 *
 * @see Grid#setCellStyleGenerator(CellStyleGenerator)
 */
public interface CellStyleGenerator extends Serializable {

    /**
     * Called by Grid to generate a style name for a column.
     *
     * @param cell
     *            the cell to generate a style for
     * @return the style name to add to this cell, or {@code null} to not
     *         set any style
     */
    public String getStyle(CellReference cell);
}