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

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tr&gt;</code> element — a row of a
 * {@link Table}.
 * <p>
 * A <code>&lt;tr&gt;</code> may only contain <code>&lt;td&gt;</code> and
 * <code>&lt;th&gt;</code> cells, so this component has no generic
 * {@code add(Component)}; put content inside the cells instead.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/tr">MDN:
 *      &lt;tr&gt; — The Table Row element</a>
 * @since 25.3
 */
@NullMarked
@Tag(Tag.TR)
public class TableRow extends HtmlComponent implements ClickNotifier<TableRow> {

    /**
     * Creates a new empty row.
     */
    public TableRow() {
        super();
    }

    /**
     * Creates a new row with the given cells.
     *
     * @param cells
     *            the cells to add.
     */
    public TableRow(TableCell... cells) {
        super();
        addCells(cells);
    }

    /**
     * Appends the given cells to this row.
     *
     * @param cells
     *            the cells to add.
     */
    public void addCells(TableCell... cells) {
        for (TableCell cell : cells) {
            getElement().appendChild(cell.getElement());
        }
    }

    /**
     * Inserts the given cell at the given position.
     *
     * @param position
     *            the position to insert the cell at, between 0 and the number
     *            of cells in this row.
     * @param cell
     *            the cell to insert.
     */
    public void insertCell(int position, TableCell cell) {
        getElement().insertChild(position, cell.getElement());
    }

    /**
     * Appends a new empty header cell to this row.
     *
     * @return the new {@code <th>}.
     */
    public TableHeaderCell addHeaderCell() {
        return append(new TableHeaderCell());
    }

    /**
     * Inserts a new empty header cell at the given position.
     *
     * @param position
     *            the position to insert the cell at, between 0 and the number
     *            of cells in this row.
     * @return the new {@code <th>}.
     */
    public TableHeaderCell insertHeaderCell(int position) {
        return insert(new TableHeaderCell(), position);
    }

    /**
     * Appends a new empty data cell to this row.
     *
     * @return the new {@code <td>}.
     */
    public TableDataCell addDataCell() {
        return append(new TableDataCell());
    }

    /**
     * Inserts a new empty data cell at the given position.
     *
     * @param position
     *            the position to insert the cell at, between 0 and the number
     *            of cells in this row.
     * @return the new {@code <td>}.
     */
    public TableDataCell insertDataCell(int position) {
        return insert(new TableDataCell(), position);
    }

    /**
     * Returns the header cells of this row, in document order.
     *
     * @return this row's {@code <th>} cells.
     */
    public List<TableHeaderCell> getHeaderCells() {
        return cellsOfType(TableHeaderCell.class);
    }

    /**
     * Returns the data cells of this row, in document order.
     *
     * @return this row's {@code <td>} cells.
     */
    public List<TableDataCell> getDataCells() {
        return cellsOfType(TableDataCell.class);
    }

    /**
     * Returns every cell of this row, in document order, both kinds combined.
     * For kind-specific lists use {@link #getHeaderCells()} or
     * {@link #getDataCells()}.
     *
     * @return this row's cells.
     */
    public List<TableCell> getCells() {
        return cellsOfType(TableCell.class);
    }

    /**
     * Removes the given cell from this row.
     *
     * @param cell
     *            the cell to remove.
     */
    public void removeCell(TableCell cell) {
        getElement().removeChild(cell.getElement());
    }

    /**
     * Removes every cell from this row.
     */
    public void removeAllCells() {
        getElement().removeAllChildren();
    }

    private <T extends Component> List<T> cellsOfType(Class<T> type) {
        return getChildren().filter(type::isInstance).map(type::cast).toList();
    }

    private <T extends TableCell> T append(T cell) {
        getElement().appendChild(cell.getElement());
        return cell;
    }

    private <T extends TableCell> T insert(T cell, int position) {
        getElement().insertChild(position, cell.getElement());
        return cell;
    }
}
