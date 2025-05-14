/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tr&gt;</code> element.
 *
 * @since 24.4
 */
@Tag(Tag.TR)
public class NativeTableRow extends HtmlContainer
        implements HasOrderedComponents, ClickNotifier<NativeTableRow> {

    /**
     * Creates a new empty table row component.
     */
    public NativeTableRow() {
        super();
    }

    /**
     * Creates a new table row with the given children components.
     *
     * @param components
     *            the children components.
     */
    public NativeTableRow(Component... components) {
        super(components);
    }

    /**
     * Add a header cell to this row.
     *
     * @return the new {@code <th>} element.
     */
    public NativeTableHeaderCell addHeaderCell() {
        NativeTableHeaderCell cell = new NativeTableHeaderCell();
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
    public NativeTableHeaderCell insertHeaderCell(int position) {
        if (position == 0) {
            return addHeaderCell();
        }
        NativeTableHeaderCell headerCell = new NativeTableHeaderCell();
        addComponentAtIndex(position, headerCell);
        return headerCell;
    }

    /**
     * Add a data cell to this row.
     *
     * @return the new {@code <td>} element.
     */
    public NativeTableCell addDataCell() {
        NativeTableCell cell = new NativeTableCell();
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
    public NativeTableCell insertDataCell(int position) {
        if (position == 0) {
            return addDataCell();
        }
        NativeTableCell nativeTableCell = new NativeTableCell();
        addComponentAtIndex(position, nativeTableCell);
        return nativeTableCell;
    }

    /**
     * Returns a list of all header cells in this row.
     *
     * @return A list of all header cells in this row.
     */
    public List<NativeTableHeaderCell> getHeaderCells() {
        return getChildren().filter(c -> c instanceof NativeTableHeaderCell)
                .map(c -> (NativeTableHeaderCell) c)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all data cells in this row.
     *
     * @return A list of all data cells in this row.
     */
    public List<NativeTableCell> getDataCells() {
        return getChildren().filter(c -> c instanceof NativeTableCell)
                .map(c -> (NativeTableCell) c).collect(Collectors.toList());
    }

    /**
     * Returns a list of all cells in this row.
     *
     * @return a list of all cells in this row.
     */
    public List<Component> getAllCells() {
        return getChildren()
                .filter(c -> c instanceof NativeTableCell
                        || c instanceof NativeTableHeaderCell)
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
    public Optional<NativeTableHeaderCell> getHeaderCell(int index) {
        return getChildren().filter(c -> c instanceof NativeTableHeaderCell)
                .map(c -> (NativeTableHeaderCell) c).skip(index).findFirst();
    }

    /**
     * Returns the data cell at a given position relative to other data cells.
     *
     * @param index
     *            the position of the data cell relative to other data cells.
     * @return the data cell at the given position (relative to other data
     *         cells).
     */
    public Optional<NativeTableCell> getDataCell(int index) {
        return getChildren().filter(c -> c instanceof NativeTableCell)
                .map(c -> (NativeTableCell) c).skip(index).findFirst();
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
        return getChildren()
                .filter(c -> c instanceof NativeTableCell
                        || c instanceof NativeTableHeaderCell)
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
    public void removeHeaderCell(NativeTableHeaderCell headerCell) {
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
    public void removeDataCell(NativeTableCell dataCell) {
        remove(dataCell);
    }

}
