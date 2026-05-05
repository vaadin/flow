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
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickNotifier;
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
     *            the cells to add — each a {@link TableDataCell} or
     *            {@link TableHeaderCell}.
     */
    public TableRow(TableCell... cells) {
        super();
        for (TableCell cell : cells) {
            getElement().appendChild(cell.getElement());
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
     * Add a header cell to this row that labels the row itself, with
     * {@code scope="row"} set on the resulting {@code <th>}. This is a
     * shortcut for the common pattern of using a leading {@code <th>} as a
     * row label, which assistive technologies announce as the header for
     * the data cells in the same row.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} element with {@code scope="row"}.
     */
    public TableHeaderCell addRowHeaderCell(String text) {
        TableHeaderCell cell = new TableHeaderCell(text);
        cell.setScope(TableHeaderCell.Scope.ROW);
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
     * Returns all cells in this row, in document order — both
     * {@link TableDataCell} and {@link TableHeaderCell} entries combined. For
     * kind-specific lists use {@link #getDataCells()} or
     * {@link #getHeaderCells()}; index into any of these lists with
     * {@code .get(i)}.
     *
     * @return a list of all cells in this row.
     */
    public List<TableCell> getCells() {
        return getChildren().filter(c -> c instanceof TableCell)
                .map(c -> (TableCell) c).collect(Collectors.toList());
    }

    /**
     * Removes a cell from this row.
     *
     * @param cell
     *            the cell to remove.
     */
    public void removeCell(TableCell cell) {
        getElement().removeChild(cell.getElement());
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
     * Appends pre-built cells to this row.
     *
     * @param cells
     *            the cells to add — each a {@link TableDataCell} or
     *            {@link TableHeaderCell}.
     * @return this row, for fluent chaining.
     */
    public TableRow addCells(TableCell... cells) {
        for (TableCell cell : cells) {
            getElement().appendChild(cell.getElement());
        }
        return this;
    }

}
