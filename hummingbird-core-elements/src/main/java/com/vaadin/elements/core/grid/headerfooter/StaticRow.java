package com.vaadin.elements.core.grid.headerfooter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.vaadin.shared.ui.grid.GridStaticSectionState.RowState;

/**
 * Abstract base class for Grid header and footer rows.
 *
 * @param <CELLTYPE>
 *            the type of the cells in the row
 */
abstract class StaticRow<CELLTYPE extends StaticSection.StaticCell>
        implements Serializable {

    private RowState rowState = new RowState();
    protected StaticSection<?> section;
    private Map<Object, CELLTYPE> cells = new LinkedHashMap<Object, CELLTYPE>();
    private Map<Set<CELLTYPE>, CELLTYPE> cellGroups = new HashMap<Set<CELLTYPE>, CELLTYPE>();

    protected StaticRow(StaticSection<?> section) {
        this.section = section;
    }

    protected void addCell(Object propertyId) {
        CELLTYPE cell = createCell();
        // FIXME
        // cell.setColumnId(
        // section.grid.getColumn(propertyId).getState().id);
        cells.put(propertyId, cell);
        rowState.cells.add(cell.getCellState());
    }

    protected void removeCell(Object propertyId) {
        CELLTYPE cell = cells.remove(propertyId);
        if (cell != null) {
            Set<CELLTYPE> cellGroupForCell = getCellGroupForCell(cell);
            if (cellGroupForCell != null) {
                removeCellFromGroup(cell, cellGroupForCell);
            }
            rowState.cells.remove(cell.getCellState());
        }
    }

    private void removeCellFromGroup(CELLTYPE cell, Set<CELLTYPE> cellGroup) {
        String columnId = cell.getColumnId();
        for (Set<String> group : rowState.cellGroups.keySet()) {
            if (group.contains(columnId)) {
                if (group.size() > 2) {
                    // Update map key correctly
                    CELLTYPE mergedCell = cellGroups.remove(cellGroup);
                    cellGroup.remove(cell);
                    cellGroups.put(cellGroup, mergedCell);

                    group.remove(columnId);
                } else {
                    rowState.cellGroups.remove(group);
                    cellGroups.remove(cellGroup);
                }
                return;
            }
        }
    }

    /**
     * Creates and returns a new instance of the cell type.
     *
     * @return the created cell
     */
    protected abstract CELLTYPE createCell();

    protected RowState getRowState() {
        return rowState;
    }

    /**
     * Returns the cell for the given property id on this row. If the column is
     * merged returned cell is the cell for the whole group.
     *
     * @param propertyId
     *            the property id of the column
     * @return the cell for the given property, merged cell for merged
     *         properties, null if not found
     */
    public CELLTYPE getCell(Object propertyId) {
        CELLTYPE cell = cells.get(propertyId);
        Set<CELLTYPE> cellGroup = getCellGroupForCell(cell);
        if (cellGroup != null) {
            cell = cellGroups.get(cellGroup);
        }
        return cell;
    }

    /**
     * Merges columns cells in a row
     *
     * @param propertyIds
     *            The property ids of columns to merge
     * @return The remaining visible cell after the merge
     */
    public CELLTYPE join(Object... propertyIds) {
        assert propertyIds.length > 1 : "You need to merge at least 2 properties";

        Set<CELLTYPE> cells = new HashSet<CELLTYPE>();
        for (int i = 0; i < propertyIds.length; ++i) {
            cells.add(getCell(propertyIds[i]));
        }

        return join(cells);
    }

    /**
     * Merges columns cells in a row
     *
     * @param cells
     *            The cells to merge. Must be from the same row.
     * @return The remaining visible cell after the merge
     */
    public CELLTYPE join(CELLTYPE... cells) {
        assert cells.length > 1 : "You need to merge at least 2 cells";

        return join(new HashSet<CELLTYPE>(Arrays.asList(cells)));
    }

    protected CELLTYPE join(Set<CELLTYPE> cells) {
        for (CELLTYPE cell : cells) {
            if (getCellGroupForCell(cell) != null) {
                throw new IllegalArgumentException("Cell already merged");
            } else if (!this.cells.containsValue(cell)) {
                throw new IllegalArgumentException(
                        "Cell does not exist on this row");
            }
        }

        // Create new cell data for the group
        CELLTYPE newCell = createCell();

        Set<String> columnGroup = new HashSet<String>();
        for (CELLTYPE cell : cells) {
            columnGroup.add(cell.getColumnId());
        }
        rowState.cellGroups.put(columnGroup, newCell.getCellState());
        cellGroups.put(cells, newCell);
        return newCell;
    }

    private Set<CELLTYPE> getCellGroupForCell(CELLTYPE cell) {
        for (Set<CELLTYPE> group : cellGroups.keySet()) {
            if (group.contains(cell)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Returns the custom style name for this row.
     *
     * @return the style name or null if no style name has been set
     */
    public String getStyleName() {
        return getRowState().styleName;
    }

    /**
     * Sets a custom style name for this row.
     *
     * @param styleName
     *            the style name to set or null to not use any style name
     */
    public void setStyleName(String styleName) {
        getRowState().styleName = styleName;
    }

    abstract protected String getCellTagName();
}