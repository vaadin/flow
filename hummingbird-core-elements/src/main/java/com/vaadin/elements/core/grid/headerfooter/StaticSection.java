package com.vaadin.elements.core.grid.headerfooter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.elements.core.grid.Grid;
import com.vaadin.shared.ui.grid.GridStaticCellType;
import com.vaadin.shared.ui.grid.GridStaticSectionState;
import com.vaadin.shared.ui.grid.GridStaticSectionState.CellState;
import com.vaadin.ui.Component;

/**
 * Abstract base class for Grid header and footer sections.
 *
 * @param <ROWTYPE>
 *            the type of the rows in the section
 */
abstract class StaticSection<ROWTYPE extends StaticRow<?>>
        implements Serializable {

    /**
     * A header or footer cell. Has a simple textual caption.
     */
    abstract static class StaticCell implements Serializable {

        private CellState cellState = new CellState();
        private StaticRow<?> row;

        protected StaticCell(StaticRow<?> row) {
            this.row = row;
        }

        void setColumnId(String id) {
            cellState.columnId = id;
        }

        String getColumnId() {
            return cellState.columnId;
        }

        /**
         * Gets the row where this cell is.
         *
         * @return row for this cell
         */
        public StaticRow<?> getRow() {
            return row;
        }

        public CellState getCellState() {
            return cellState;
        }

        /**
         * Sets the text displayed in this cell.
         *
         * @param text
         *            a plain text caption
         */
        public void setText(String text) {
            removeComponentIfPresent();
            cellState.text = text;
            cellState.type = GridStaticCellType.TEXT;
            row.section.markAsDirty();
        }

        /**
         * Returns the text displayed in this cell.
         *
         * @return the plain text caption
         */
        public String getText() {
            if (cellState.type != GridStaticCellType.TEXT) {
                throw new IllegalStateException(
                        "Cannot fetch Text from a cell with type "
                                + cellState.type);
            }
            return cellState.text;
        }

        /**
         * Returns the HTML content displayed in this cell.
         *
         * @return the html
         *
         */
        public String getHtml() {
            if (cellState.type != GridStaticCellType.HTML) {
                throw new IllegalStateException(
                        "Cannot fetch HTML from a cell with type "
                                + cellState.type);
            }
            return cellState.html;
        }

        /**
         * Sets the HTML content displayed in this cell.
         *
         * @param html
         *            the html to set
         */
        public void setHtml(String html) {
            removeComponentIfPresent();
            cellState.html = html;
            cellState.type = GridStaticCellType.HTML;
            row.section.markAsDirty();
        }

        /**
         * Returns the component displayed in this cell.
         *
         * @return the component
         */
        public Component getComponent() {
            if (cellState.type != GridStaticCellType.WIDGET) {
                throw new IllegalStateException(
                        "Cannot fetch Component from a cell with type "
                                + cellState.type);
            }
            return (Component) cellState.connector;
        }

        /**
         * Sets the component displayed in this cell.
         *
         * @param component
         *            the component to set
         */
        public void setComponent(Component component) {
            removeComponentIfPresent();
            component.setParent(row.section.grid);
            cellState.connector = component;
            cellState.type = GridStaticCellType.WIDGET;
            row.section.markAsDirty();
        }

        /**
         * Returns the type of content stored in this cell.
         *
         * @return cell content type
         */
        public GridStaticCellType getCellType() {
            return cellState.type;
        }

        /**
         * Returns the custom style name for this cell.
         *
         * @return the style name or null if no style name has been set
         */
        public String getStyleName() {
            return cellState.styleName;
        }

        /**
         * Sets a custom style name for this cell.
         *
         * @param styleName
         *            the style name to set or null to not use any style name
         */
        public void setStyleName(String styleName) {
            cellState.styleName = styleName;
            row.section.markAsDirty();
        }

        private void removeComponentIfPresent() {
            Component component = (Component) cellState.connector;
            if (component != null) {
                component.setParent(null);
                cellState.connector = null;
            }
        }

    }

    protected Grid grid;
    protected List<ROWTYPE> rows = new ArrayList<ROWTYPE>();

    /**
     * Sets the visibility of the whole section.
     *
     * @param visible
     *            true to show this section, false to hide
     */
    public void setVisible(boolean visible) {
        if (getSectionState().visible != visible) {
            getSectionState().visible = visible;
            markAsDirty();
        }
    }

    /**
     * Returns the visibility of this section.
     *
     * @return true if visible, false otherwise.
     */
    public boolean isVisible() {
        return getSectionState().visible;
    }

    /**
     * Removes the row at the given position.
     *
     * @param rowIndex
     *            the position of the row
     *
     * @throws IllegalArgumentException
     *             if no row exists at given index
     * @see #removeRow(StaticRow)
     * @see #addRowAt(int)
     * @see #appendRow()
     * @see #prependRow()
     */
    public ROWTYPE removeRow(int rowIndex) {
        if (rowIndex >= rows.size() || rowIndex < 0) {
            throw new IllegalArgumentException(
                    "No row at given index " + rowIndex);
        }
        ROWTYPE row = rows.remove(rowIndex);
        getSectionState().rows.remove(rowIndex);

        markAsDirty();
        return row;
    }

    /**
     * Removes the given row from the section.
     *
     * @param row
     *            the row to be removed
     *
     * @throws IllegalArgumentException
     *             if the row does not exist in this section
     * @see #removeRow(int)
     * @see #addRowAt(int)
     * @see #appendRow()
     * @see #prependRow()
     */
    public void removeRow(ROWTYPE row) {
        try {
            removeRow(rows.indexOf(row));
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    "Section does not contain the given row");
        }
    }

    /**
     * Gets row at given index.
     *
     * @param rowIndex
     *            0 based index for row. Counted from top to bottom
     * @return row at given index
     */
    public ROWTYPE getRow(int rowIndex) {
        if (rowIndex >= rows.size() || rowIndex < 0) {
            throw new IllegalArgumentException(
                    "No row at given index " + rowIndex);
        }
        return rows.get(rowIndex);
    }

    /**
     * Adds a new row at the top of this section.
     *
     * @return the new row
     * @see #appendRow()
     * @see #addRowAt(int)
     * @see #removeRow(StaticRow)
     * @see #removeRow(int)
     */
    public ROWTYPE prependRow() {
        return addRowAt(0);
    }

    /**
     * Adds a new row at the bottom of this section.
     *
     * @return the new row
     * @see #prependRow()
     * @see #addRowAt(int)
     * @see #removeRow(StaticRow)
     * @see #removeRow(int)
     */
    public ROWTYPE appendRow() {
        return addRowAt(rows.size());
    }

    /**
     * Inserts a new row at the given position.
     *
     * @param index
     *            the position at which to insert the row
     * @return the new row
     *
     * @throws IndexOutOfBoundsException
     *             if the index is out of bounds
     * @see #appendRow()
     * @see #prependRow()
     * @see #removeRow(StaticRow)
     * @see #removeRow(int)
     */
    public ROWTYPE addRowAt(int index) {
        if (index > rows.size() || index < 0) {
            throw new IllegalArgumentException(
                    "Unable to add row at index " + index);
        }
        ROWTYPE row = createRow();
        rows.add(index, row);
        getSectionState().rows.add(index, row.getRowState());

        for (Object id : grid.columns.keySet()) {
            row.addCell(id);
        }

        markAsDirty();
        return row;
    }

    /**
     * Gets the amount of rows in this section.
     *
     * @return row count
     */
    public int getRowCount() {
        return rows.size();
    }

    protected abstract GridStaticSectionState getSectionState();

    protected abstract ROWTYPE createRow();

    /**
     * Informs the grid that state has changed and it should be redrawn.
     */
    protected void markAsDirty() {
        grid.markAsDirty();
    }

    /**
     * Removes a column for given property id from the section.
     *
     * @param propertyId
     *            property to be removed
     */
    public void removeColumn(Object propertyId) {
        for (ROWTYPE row : rows) {
            row.removeCell(propertyId);
        }
    }

    /**
     * Adds a column for given property id to the section.
     *
     * @param propertyId
     *            property to be added
     */
    public void addColumn(Object propertyId) {
        for (ROWTYPE row : rows) {
            row.addCell(propertyId);
        }
    }

    /**
     * Performs a sanity check that section is in correct state.
     *
     * @throws IllegalStateException
     *             if merged cells are not i n continuous range
     */
    protected void sanityCheck() throws IllegalStateException {
        // List<String> columnOrder = grid.getState().columnOrder;
        // for (ROWTYPE row : rows) {
        // for (Set<String> cellGroup : row.getRowState().cellGroups
        // .keySet()) {
        // if (!checkCellGroupAndOrder(columnOrder, cellGroup)) {
        // throw new IllegalStateException(
        // "Not all merged cells were in a continuous range.");
        // }
        // }
        // }
    }

    private boolean checkCellGroupAndOrder(List<String> columnOrder,
            Set<String> cellGroup) {
        if (!columnOrder.containsAll(cellGroup)) {
            return false;
        }

        for (int i = 0; i < columnOrder.size(); ++i) {
            if (!cellGroup.contains(columnOrder.get(i))) {
                continue;
            }

            for (int j = 1; j < cellGroup.size(); ++j) {
                if (!cellGroup.contains(columnOrder.get(i + j))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}