/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.component.html;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tr&gt;</code> element.
 *
 * @since 24.4
 */
@Tag(Tag.TR)
public class TableRow extends HtmlComponent
        implements HasOrderedComponents, ClickNotifier<TableRow> {

    /**
     * Creates a new empty table row component.
     */
    public TableRow() {
        super();
    }

    /**
     * Creates a new table row with the given children components.
     *
     * @param components
     *            the children components.
     */
    public TableRow(Component... components) {
        super();
        add(components);
    }

    /**
     * Add a header cell to this row.
     *
     * @return the new {@code <th>} element.
     */
    public TableHeaderCell addHeaderCell() {
        TableHeaderCell cell = new TableHeaderCell();
        add(cell);
        return cell;
    }

    /**
     * Insert a new header cell into a given position.
     *
     * @param position
     *            the position into which the header cell must be added.
     * @return the new header cell.
     */
    public TableHeaderCell insertHeaderCell(int position) {
        if (position == 0) {
            return addHeaderCell();
        }
        TableHeaderCell headerCell = new TableHeaderCell();
        addComponentAtIndex(position, headerCell);
        return headerCell;
    }

    /**
     * Add a data cell to this row.
     *
     * @return the new {@code <td>} element.
     */
    public TableCell addDataCell() {
        TableCell cell = new TableCell();
        add(cell);
        return cell;
    }

    /**
     * Insert a new data cell into a given position.
     *
     * @param position
     *            the position into which the data cell must be added.
     * @return the new data cell.
     */
    public TableCell insertDataCell(int position) {
        if (position == 0) {
            return addDataCell();
        }
        TableCell tableCell = new TableCell();
        addComponentAtIndex(position, tableCell);
        return tableCell;
    }

    /**
     * Returns a list of all header cells in this row.
     *
     * @return A list of all header cells in this row.
     */
    public List<TableHeaderCell> getHeaderCells() {
        return getChildren().filter(c -> c instanceof TableHeaderCell)
                .map(c -> (TableHeaderCell) c).collect(Collectors.toList());
    }

    /**
     * Returns a list of all data cells in this row.
     *
     * @return A list of all data cells in this row.
     */
    public List<TableCell> getDataCells() {
        return getChildren().filter(c -> c instanceof TableCell)
                .map(c -> (TableCell) c).collect(Collectors.toList());
    }

    /**
     * Returns a list of all cells in this row.
     *
     * @return a list of all cells in this row.
     */
    public List<Component> getAllCells() {
        return getChildren().filter(
                c -> c instanceof TableCell || c instanceof TableHeaderCell)
                .collect(Collectors.toList());
    }

    /**
     * Returns the header cell at a given position relative to other header
     * cells.
     *
     * @param index
     *            the position of the header cell relative to other header
     *            cells.
     * @return the header cell at the given position (relative to other header
     *         cells).
     */
    public Optional<TableHeaderCell> getHeaderCell(int index) {
        return getChildren().filter(c -> c instanceof TableHeaderCell)
                .map(c -> (TableHeaderCell) c).skip(index).findFirst();
    }

    /**
     * Returns the data cell at a given position relative to other data cells.
     *
     * @param index
     *            the position of the data cell relative to other data cells.
     * @return the data cell at the given position (relative to other data
     *         cells).
     */
    public Optional<TableCell> getDataCell(int index) {
        return getChildren().filter(c -> c instanceof TableCell)
                .map(c -> (TableCell) c).skip(index).findFirst();
    }

    /**
     * Returns the cell at a given position.
     *
     * @param index
     *            the position of the cell.
     * @return the cell at the given position
     * @throws IndexOutOfBoundsException
     *             if index is negative or greater than (or equal to) the number
     *             of cells in the row
     */
    public Optional<Component> getCell(int index) {
        return getChildren().filter(
                c -> c instanceof TableCell || c instanceof TableHeaderCell)
                .skip(index).findFirst();
    }

    /**
     * Removes the cell at a given position.
     *
     * @param index
     *            the position of the cell to remove
     */
    public void removeCell(int index) {
        getCell(index).ifPresent(this::remove);
    }

    /**
     * Removes the header cell at a position relative to other header cells.
     *
     * @param index
     *            the position of the header cell relative to other header
     *            cells.
     */
    public void removeHeaderCell(int index) {
        getHeaderCell(index).ifPresent(this::remove);
    }

    /**
     * Removes a header cell.
     *
     * @param headerCell
     *            the header cell to remove.
     */
    public void removeHeaderCell(TableHeaderCell headerCell) {
        remove(headerCell);
    }

    /**
     * Removes the data cell at a given position relative to other data cells.
     *
     * @param index
     *            the position of the data cell to remove relative to other data
     *            cells.
     */
    public void removeDataCell(int index) {
        getDataCell(index).ifPresent(this::remove);
    }

    /**
     * Removes a data cell.
     *
     * @param dataCell
     *            the data cell to remove.
     */
    public void removeDataCell(TableCell dataCell) {
        remove(dataCell);
    }

}
