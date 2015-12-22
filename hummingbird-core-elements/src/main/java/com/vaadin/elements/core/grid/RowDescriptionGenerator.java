package com.vaadin.elements.core.grid;

import java.io.Serializable;

/**
 * A callback interface for generating optional descriptions (tooltips) for
 * Grid rows. If a description is generated for a row, it is used for all
 * the cells in the row for which a {@link CellDescriptionGenerator cell
 * description} is not generated.
 *
 * @see Grid#setRowDescriptionGenerator(CellDescriptionGenerator)
 *
 * @since 7.6
 */
public interface RowDescriptionGenerator extends Serializable {

    /**
     * Called by Grid to generate a description (tooltip) for a row. The
     * description may contain HTML which is rendered directly; if this is
     * not desired the returned string must be escaped by the implementing
     * method.
     *
     * @param row
     *            the row to generate a description for
     * @return the row description or {@code null} for no description
     */
    public String getDescription(RowReference row);
}