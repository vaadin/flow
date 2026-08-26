/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tr&gt;</code> element.
 *
 * @since 24.5
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
     * Add a header cell to this row with the given text content.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} element.
     */
    public NativeTableHeaderCell addHeaderCell(String text) {
        NativeTableHeaderCell cell = new NativeTableHeaderCell(text);
        add(cell);
        return cell;
    }

    /**
     * Add a header cell to this row that labels the row itself, with
     * {@code scope="row"} set on the resulting {@code <th>}. This is a
     * shortcut for the common pattern of using a leading {@code <th>} as a row
     * label, which assistive technologies announce as the header for the data
     * cells in the same row.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} element with {@code scope="row"}.
     */
    public NativeTableHeaderCell addRowHeaderCell(String text) {
        NativeTableHeaderCell cell = addHeaderCell(text);
        cell.setScope(NativeTableHeaderCell.Scope.ROW);
        return cell;
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
     * Add a data cell to this row with the given text content.
     *
     * @param text
     *            the text content.
     * @return the new {@code <td>} element.
     */
    public NativeTableCell addDataCell(String text) {
        NativeTableCell cell = new NativeTableCell(text);
        add(cell);
        return cell;
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


    /**
     * Appends a sequence of data cells ({@code <td>}) with the given text
     * contents to this row.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return this row, for fluent chaining.
     */
    public NativeTableRow addDataCells(String... cellTexts) {
        return addDataCells(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addDataCells(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return this row, for fluent chaining.
     */
    public NativeTableRow addDataCells(List<String> cellTexts) {
        cellTexts.forEach(this::addDataCell);
        return this;
    }

    /**
     * Appends a sequence of header cells ({@code <th>}) with the given text
     * contents to this row.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return this row, for fluent chaining.
     */
    public NativeTableRow addHeaderCells(String... cellTexts) {
        return addHeaderCells(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addHeaderCells(String...)}.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return this row, for fluent chaining.
     */
    public NativeTableRow addHeaderCells(List<String> cellTexts) {
        cellTexts.forEach(this::addHeaderCell);
        return this;
    }

    /**
     * Appends children to this row. A {@link NativeTableCell} or
     * {@link NativeTableHeaderCell} argument is added as-is; any other
     * component is wrapped in a new {@link NativeTableCell}, since a
     * <code>&lt;tr&gt;</code> may only contain {@code <td>} and {@code <th>}
     * elements.
     *
     * @param components
     *            the cells (used as-is) or other components (wrapped in
     *            {@code <td>}) to append.
     * @return this row, for fluent chaining.
     */
    public NativeTableRow addCells(Component... components) {
        return addCells(Arrays.asList(components));
    }

    /**
     * List equivalent of {@link #addCells(Component...)}.
     *
     * @param components
     *            the cells or wrap-target components.
     * @return this row, for fluent chaining.
     */
    public NativeTableRow addCells(List<? extends Component> components) {
        for (Component component : components) {
            add(isCell(component) ? component
                    : new NativeTableCell(component));
        }
        return this;
    }

    private static boolean isCell(Component component) {
        return component instanceof NativeTableCell
                || component instanceof NativeTableHeaderCell;
    }
}
