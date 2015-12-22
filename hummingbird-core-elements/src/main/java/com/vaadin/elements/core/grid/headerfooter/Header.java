package com.vaadin.elements.core.grid.headerfooter;

import com.vaadin.elements.core.grid.Grid;
import com.vaadin.shared.ui.grid.GridStaticSectionState;

/**
 * Represents the header section of a Grid.
 */
public class Header extends StaticSection<HeaderRow> {

    private HeaderRow defaultRow = null;
    private final GridStaticSectionState headerState = new GridStaticSectionState();

    public Header(Grid grid) {
        this.grid = grid;
        grid.getState(true).header = headerState;
        HeaderRow row = createRow();
        rows.add(row);
        setDefaultRow(row);
        getSectionState().rows.add(row.getRowState());
    }

    /**
     * Sets the default row of this header. The default row is a special header
     * row providing a user interface for sorting columns.
     *
     * @param row
     *            the new default row, or null for no default row
     *
     * @throws IllegalArgumentException
     *             this header does not contain the row
     */
    public void setDefaultRow(HeaderRow row) {
        if (row == defaultRow) {
            return;
        }

        if (row != null && !rows.contains(row)) {
            throw new IllegalArgumentException(
                    "Cannot set a default row that does not exist in the section");
        }

        if (defaultRow != null) {
            defaultRow.setDefaultRow(false);
        }

        if (row != null) {
            row.setDefaultRow(true);
        }

        defaultRow = row;
        markAsDirty();
    }

    /**
     * Returns the current default row of this header. The default row is a
     * special header row providing a user interface for sorting columns.
     *
     * @return the default row or null if no default row set
     */
    public HeaderRow getDefaultRow() {
        return defaultRow;
    }

    @Override
    protected GridStaticSectionState getSectionState() {
        return headerState;
    }

    @Override
    protected HeaderRow createRow() {
        return new HeaderRow(this);
    }

    @Override
    public HeaderRow removeRow(int rowIndex) {
        HeaderRow row = super.removeRow(rowIndex);
        if (row == defaultRow) {
            // Default Header Row was just removed.
            setDefaultRow(null);
        }
        return row;
    }

    @Override
    public void sanityCheck() throws IllegalStateException {
        super.sanityCheck();

        boolean hasDefaultRow = false;
        for (HeaderRow row : rows) {
            if (row.getRowState().defaultRow) {
                if (!hasDefaultRow) {
                    hasDefaultRow = true;
                } else {
                    throw new IllegalStateException(
                            "Multiple default rows in header");
                }
            }
        }
    }

}