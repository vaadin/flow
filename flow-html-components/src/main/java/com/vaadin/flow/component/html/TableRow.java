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

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
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
     * Creates a new row with the given content, following the rules of
     * {@link #addCells(Component...)}.
     *
     * @param components
     *            the cells, or the content to wrap in cells.
     */
    public TableRow(Component... components) {
        super();
        addCells(components);
    }

    /**
     * List equivalent of {@link #TableRow(Component...)}.
     *
     * @param components
     *            the cells, or the content to wrap in cells.
     */
    public TableRow(List<? extends Component> components) {
        super();
        addCells(components);
    }

    /**
     * Appends the given components to this row. A {@link TableCell} is added
     * as-is; anything else is wrapped in a new {@link TableDataCell}, since a
     * <code>&lt;tr&gt;</code> may only contain cells.
     *
     * @param components
     *            the cells, or the content to wrap in cells.
     * @return this row, for fluent chaining.
     */
    public TableRow addCells(Component... components) {
        return addCells(Arrays.asList(components));
    }

    /**
     * List equivalent of {@link #addCells(Component...)}.
     *
     * @param components
     *            the cells, or the content to wrap in cells.
     * @return this row, for fluent chaining.
     */
    public TableRow addCells(List<? extends Component> components) {
        for (Component component : components) {
            if (component instanceof TableCell cell) {
                append(cell);
            } else {
                append(new TableDataCell(component));
            }
        }
        return this;
    }

    /**
     * Appends a sequence of data cells (<code>&lt;td&gt;</code>) with the given
     * text contents to this row.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return this row, for fluent chaining.
     */
    public TableRow addDataCells(String... cellTexts) {
        return addDataCells(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addDataCells(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return this row, for fluent chaining.
     */
    public TableRow addDataCells(List<String> cellTexts) {
        cellTexts.forEach(this::addDataCell);
        return this;
    }

    /**
     * Appends a sequence of header cells (<code>&lt;th&gt;</code>) with the
     * given text contents to this row.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return this row, for fluent chaining.
     */
    public TableRow addHeaderCells(String... cellTexts) {
        return addHeaderCells(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addHeaderCells(String...)}.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return this row, for fluent chaining.
     */
    public TableRow addHeaderCells(List<String> cellTexts) {
        cellTexts.forEach(this::addHeaderCell);
        return this;
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
     * Appends a new header cell with the given text to this row.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>}.
     */
    public TableHeaderCell addHeaderCell(String text) {
        return append(new TableHeaderCell(text));
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
     * Appends a header cell to this row that labels the row itself, with
     * {@code scope="row"} set on the resulting <code>&lt;th&gt;</code>. This is
     * a shortcut for the common pattern of using a leading
     * <code>&lt;th&gt;</code> as a row label, which assistive technologies
     * announce as the header for the data cells in the same row.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} with {@code scope="row"}.
     */
    public TableHeaderCell addRowHeaderCell(String text) {
        TableHeaderCell cell = addHeaderCell(text);
        cell.setScope(TableHeaderCell.Scope.ROW);
        return cell;
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
     * Appends a new data cell with the given text to this row.
     *
     * @param text
     *            the text content.
     * @return the new {@code <td>}.
     */
    public TableDataCell addDataCell(String text) {
        return append(new TableDataCell(text));
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
        return ComponentUtil.getChildrenOfType(this, type).toList();
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
