package com.vaadin.elements.core.grid;

import java.io.Serializable;

/**
 * A callback interface for generating optional descriptions (tooltips) for Grid
 * cells. If a cell has both a {@link RowDescriptionGenerator row description}
 *  and a cell description, the latter has precedence.
 *
 * @see Grid#setCellDescriptionGenerator(CellDescriptionGenerator)
 *
 * @since 7.6
 */
public interface CellDescriptionGenerator extends Serializable {

    /**
     * Called by Grid to generate a description (tooltip) for a cell. The
     * description may contain HTML which is rendered directly; if this is not
     * desired the returned string must be escaped by the implementing method.
     *
     * @param cell
     *            the cell to generate a description for
     * @return the cell description or {@code null} for no description
     */
    public String getDescription(CellReference cell);
}