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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tr&gt;</code> element.
 * <p>
 * Per the
 * <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG HTML
 * specification</a>, a {@code <tr>} may only contain {@code <td>} or
 * {@code <th>} elements. This component therefore extends
 * {@link HtmlComponent} (rather than
 * {@link com.vaadin.flow.component.HtmlContainer}) and exposes only methods for
 * adding {@link TableDataCell} and {@link TableHeaderCell} children.
 *
 * @since 25.2
 */
@Tag(Tag.TR)
public class TableRow extends HtmlComponent
        implements ClickNotifier<TableRow> {

    /**
     * Creates a new empty table row component.
     */
    public TableRow() {
        super();
    }

    /**
     * Creates a new table row with the given cells.
     *
     * @param cells
     *            the cells to add. Each cell must be either a {@link TableDataCell}
     *            or a {@link TableHeaderCell}.
     */
    public TableRow(Component... cells) {
        super();
        for (Component cell : cells) {
            if (cell instanceof TableDataCell || cell instanceof TableHeaderCell) {
                getElement().appendChild(cell.getElement());
            } else {
                throw new IllegalArgumentException(
                        "A <tr> may only contain <td> (TableDataCell) or <th> "
                                + "(TableHeaderCell) children, not "
                                + cell.getClass().getName());
            }
        }
    }

    /**
     * Add a header cell to this row.
     *
     * @return the new {@code <th>} element.
     */
    public TableHeaderCell addHeaderCell() {
        TableHeaderCell cell = new TableHeaderCell();
        getElement().appendChild(cell.getElement());
        return cell;
    }

    /**
     * Add a header cell to this row with the given text content.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} element.
     */
    public TableHeaderCell addHeaderCell(String text) {
        TableHeaderCell cell = new TableHeaderCell(text);
        getElement().appendChild(cell.getElement());
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
        TableHeaderCell headerCell = new TableHeaderCell();
        getElement().insertChild(position, headerCell.getElement());
        return headerCell;
    }

    /**
     * Add a data cell to this row.
     *
     * @return the new {@code <td>} element.
     */
    public TableDataCell addDataCell() {
        TableDataCell cell = new TableDataCell();
        getElement().appendChild(cell.getElement());
        return cell;
    }

    /**
     * Add a data cell to this row with the given text content.
     *
     * @param text
     *            the text content.
     * @return the new {@code <td>} element.
     */
    public TableDataCell addDataCell(String text) {
        TableDataCell cell = new TableDataCell(text);
        getElement().appendChild(cell.getElement());
        return cell;
    }

    /**
     * Insert a new data cell into a given position.
     *
     * @param position
     *            the position into which the data cell must be added.
     * @return the new data cell.
     */
    public TableDataCell insertDataCell(int position) {
        TableDataCell tableCell = new TableDataCell();
        getElement().insertChild(position, tableCell.getElement());
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
    public List<TableDataCell> getDataCells() {
        return getChildren().filter(c -> c instanceof TableDataCell)
                .map(c -> (TableDataCell) c).collect(Collectors.toList());
    }

    /**
     * Returns a list of all cells in this row.
     *
     * @return a list of all cells in this row.
     */
    public List<Component> getAllCells() {
        return getChildren()
                .filter(c -> c instanceof TableDataCell
                        || c instanceof TableHeaderCell)
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
    public Optional<TableDataCell> getDataCell(int index) {
        return getChildren().filter(c -> c instanceof TableDataCell)
                .map(c -> (TableDataCell) c).skip(index).findFirst();
    }

    /**
     * Returns the cell at a given position.
     *
     * @param index
     *            the position of the cell.
     * @return the cell at the given position
     */
    public Optional<Component> getCell(int index) {
        return getChildren()
                .filter(c -> c instanceof TableDataCell
                        || c instanceof TableHeaderCell)
                .skip(index).findFirst();
    }

    /**
     * Removes the cell at a given position.
     *
     * @param index
     *            the position of the cell to remove
     */
    public void removeCell(int index) {
        getCell(index).ifPresent(
                c -> getElement().removeChild(c.getElement()));
    }

    /**
     * Removes the header cell at a position relative to other header cells.
     *
     * @param index
     *            the position of the header cell relative to other header
     *            cells.
     */
    public void removeHeaderCell(int index) {
        getHeaderCell(index).ifPresent(
                c -> getElement().removeChild(c.getElement()));
    }

    /**
     * Removes a header cell.
     *
     * @param headerCell
     *            the header cell to remove.
     */
    public void removeHeaderCell(TableHeaderCell headerCell) {
        getElement().removeChild(headerCell.getElement());
    }

    /**
     * Removes the data cell at a given position relative to other data cells.
     *
     * @param index
     *            the position of the data cell to remove relative to other data
     *            cells.
     */
    public void removeDataCell(int index) {
        getDataCell(index).ifPresent(
                c -> getElement().removeChild(c.getElement()));
    }

    /**
     * Removes a data cell.
     *
     * @param dataCell
     *            the data cell to remove.
     */
    public void removeDataCell(TableDataCell dataCell) {
        getElement().removeChild(dataCell.getElement());
    }

    /**
     * Appends a sequence of data cells ({@code <td>}) with the given text
     * contents to this row.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return this row, for fluent chaining.
     */
    public TableRow addDataCells(String... cellTexts) {
        for (String text : cellTexts) {
            addDataCell(text);
        }
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
    public TableRow addHeaderCells(String... cellTexts) {
        for (String text : cellTexts) {
            addHeaderCell(text);
        }
        return this;
    }

    /**
     * Appends pre-built cells to this row. Each cell must be either a
     * {@link TableDataCell} or a {@link TableHeaderCell}.
     *
     * @param cells
     *            the cells to add.
     * @return this row, for fluent chaining.
     * @throws IllegalArgumentException
     *             if any of the given components is neither a
     *             {@link TableDataCell} nor a {@link TableHeaderCell}.
     */
    public TableRow addCells(Component... cells) {
        for (Component cell : cells) {
            if (cell instanceof TableDataCell || cell instanceof TableHeaderCell) {
                getElement().appendChild(cell.getElement());
            } else {
                throw new IllegalArgumentException(
                        "A <tr> may only contain <td> (TableDataCell) or <th> "
                                + "(TableHeaderCell) children, not "
                                + cell.getClass().getName());
            }
        }
        return this;
    }

}
