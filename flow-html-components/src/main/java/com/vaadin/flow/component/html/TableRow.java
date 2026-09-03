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
import com.vaadin.flow.component.HasComponentsOfType;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.signals.Signal;

/**
 * Component representing a <code>&lt;tr&gt;</code> element — a row of a
 * {@link Table}.
 * <p>
 * A <code>&lt;tr&gt;</code> may only contain <code>&lt;td&gt;</code> and
 * <code>&lt;th&gt;</code> cells, which is what {@link HasComponentsOfType}
 * expresses: the standard {@code add}, {@code remove}, {@code replace} and
 * {@link HasComponentsOfType#bindChildren(com.vaadin.flow.signals.Signal, com.vaadin.flow.function.SerializableFunction)
 * bindChildren} operations are all available, but only for {@link TableCell},
 * so anything else is rejected at compile time. What this class adds on top are
 * the cell factories, which create a cell and attach it in one call. To put
 * arbitrary content in a row, put it inside a cell.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/tr">MDN:
 *      &lt;tr&gt; — The Table Row element</a>
 * @since 25.3
 */
@NullMarked
@Tag(Tag.TR)
public class TableRow extends HtmlComponent
        implements HasComponentsOfType<TableCell>, ClickNotifier<TableRow> {

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
     * <code>&lt;tr&gt;</code> may only contain cells. This is the lenient
     * counterpart of the inherited {@code add(TableCell...)}, which takes cells
     * only and rejects anything else at compile time.
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
                add(cell);
            } else {
                add(new TableDataCell(component));
            }
        }
        return this;
    }

    /**
     * Appends a sequence of data cells (<code>&lt;td&gt;</code>) with the given
     * text contents to this row.
     *
     * <p>
     * For example, {@code addDataCells("4.87", "12,104")} renders as:
     *
     * <pre>{@code
     * <td>4.87</td>
     * <td>12,104</td>
     * }</pre>
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
     * <p>
     * For example, {@code addHeaderCells("Mon", "Tue")} renders as:
     *
     * <pre>{@code
     * <th>Mon</th>
     * <th>Tue</th>
     * }</pre>
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
     * Appends a header cell for each of the given texts, each labelling the
     * column it sits in with {@code scope="col"}. This is the bulk form of
     * {@link #addColumnHeaderCell(String)} and the usual way to fill a row in a
     * {@link TableHead}.
     *
     * <p>
     * For example, {@code addColumnHeaderCells("Mon", "Tue")} renders as:
     *
     * <pre>{@code
     * <th scope="col">Mon</th>
     * <th scope="col">Tue</th>
     * }</pre>
     *
     * @param cellTexts
     *            the text content of the cells, one cell per entry.
     * @return this row, for chaining.
     */
    public TableRow addColumnHeaderCells(String... cellTexts) {
        return addColumnHeaderCells(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addColumnHeaderCells(String...)}.
     *
     * @param cellTexts
     *            the text content of the cells, one cell per entry.
     * @return this row, for chaining.
     */
    public TableRow addColumnHeaderCells(List<String> cellTexts) {
        cellTexts.forEach(this::addColumnHeaderCell);
        return this;
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
     * Appends a new header cell with the given text to this row. The cell
     * carries no {@code scope}; use {@link #addRowHeaderCell(String)} or
     * {@link #addColumnHeaderCell(String)} when the cell labels a row or a
     * column.
     * <p>
     * A span goes on the cell handed back, so
     * {@code row.addHeaderCell("Animals").setColspan(2)} is a single statement.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>}.
     */
    public TableHeaderCell addHeaderCell(String text) {
        return append(new TableHeaderCell(text));
    }

    /**
     * Appends a new header cell whose text content follows the given signal.
     * <p>
     * The cell follows the signal for as long as it is attached, so this is the
     * factory to use for a header whose label is reactive rather than fixed.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @return the new {@code <th>}.
     */
    public TableHeaderCell addHeaderCell(Signal<String> textSignal) {
        return append(new TableHeaderCell(textSignal));
    }

    /**
     * Appends a new header cell holding the given content to this row, for a
     * header that is more than plain text.
     *
     * @param content
     *            the content of the cell.
     * @return the new {@code <th>}.
     */
    public TableHeaderCell addHeaderCell(Component... content) {
        return append(new TableHeaderCell(content));
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
     * <p>
     * For example, {@code addRowHeaderCell("Breed")} renders as:
     *
     * <pre>{@code
     * <th scope="row">Breed</th>
     * }</pre>
     * <p>
     * There is no overload taking a span, because a span goes on the cell
     * handed back — {@code row.addRowHeaderCell("Horse").setRowspan(2)} — and
     * because it would be ambiguous which of the two spans an {@code int}
     * meant. {@link #addRowGroupHeaderCell(String, int)} and
     * {@link #addColumnGroupHeaderCell(String, int)} do take one: there the
     * span is what makes the cell a group header rather than decoration on it.
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
     * Appends a header cell labelling the row it leads, with
     * {@code scope="row"} set on the resulting <code>&lt;th&gt;</code> and its
     * text content following the given signal.
     * <p>
     * The cell follows the signal for as long as it is attached, so this is the
     * factory to use for a header whose label is reactive rather than fixed.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @return the new {@code <th>} with {@code scope="row"}.
     */
    public TableHeaderCell addRowHeaderCell(Signal<String> textSignal) {
        TableHeaderCell cell = addHeaderCell(textSignal);
        cell.setScope(TableHeaderCell.Scope.ROW);
        return cell;
    }

    /**
     * Appends a header cell holding the given content and labelling the row it
     * leads, with {@code scope="row"} set on the resulting
     * <code>&lt;th&gt;</code>. This is the content-taking form of
     * {@link #addRowHeaderCell(String)}, for a label that is not plain text.
     * <p>
     * For example, {@code addRowHeaderCell(new Span("Breed"))} renders as:
     *
     * <pre>{@code
     * <th scope="row"><span>Breed</span></th>
     * }</pre>
     *
     * @param content
     *            the content of the cell.
     * @return the new {@code <th>} with {@code scope="row"}.
     */
    public TableHeaderCell addRowHeaderCell(Component... content) {
        TableHeaderCell cell = addHeaderCell(content);
        cell.setScope(TableHeaderCell.Scope.ROW);
        return cell;
    }

    /**
     * Appends a header cell labelling the column it sits in, with
     * {@code scope="col"} set on the resulting <code>&lt;th&gt;</code>. This is
     * the counterpart of {@link #addRowHeaderCell(String)} and the usual shape
     * of a cell in a {@link TableHead}.
     *
     * <p>
     * For example, {@code addColumnHeaderCell("Name")} renders as:
     *
     * <pre>{@code
     * <th scope="col">Name</th>
     * }</pre>
     * <p>
     * A span goes on the cell handed back, as it does for
     * {@link #addRowHeaderCell(String)}; a header that spans the columns it
     * names is usually better expressed with
     * {@link #addColumnGroupHeaderCell(String, int)}.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} with {@code scope="col"}.
     */
    public TableHeaderCell addColumnHeaderCell(String text) {
        TableHeaderCell cell = addHeaderCell(text);
        cell.setScope(TableHeaderCell.Scope.COL);
        return cell;
    }

    /**
     * Appends a header cell labelling the column it sits in, with
     * {@code scope="col"} set on the resulting <code>&lt;th&gt;</code> and its
     * text content following the given signal.
     * <p>
     * The cell follows the signal for as long as it is attached, so this is the
     * factory to use for a header whose label is reactive rather than fixed.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @return the new {@code <th>} with {@code scope="col"}.
     */
    public TableHeaderCell addColumnHeaderCell(Signal<String> textSignal) {
        TableHeaderCell cell = addHeaderCell(textSignal);
        cell.setScope(TableHeaderCell.Scope.COL);
        return cell;
    }

    /**
     * Appends a header cell holding the given content and labelling the column
     * it sits in, with {@code scope="col"} set on the resulting
     * <code>&lt;th&gt;</code>. This is the content-taking form of
     * {@link #addColumnHeaderCell(String)}, for a label that is not plain text.
     * <p>
     * For example, {@code addColumnHeaderCell(new Span("Name"))} renders as:
     *
     * <pre>{@code
     * <th scope="col"><span>Name</span></th>
     * }</pre>
     *
     * @param content
     *            the content of the cell.
     * @return the new {@code <th>} with {@code scope="col"}.
     */
    public TableHeaderCell addColumnHeaderCell(Component... content) {
        TableHeaderCell cell = addHeaderCell(content);
        cell.setScope(TableHeaderCell.Scope.COL);
        return cell;
    }

    /**
     * Appends a header cell labelling the band of rows it heads, with
     * {@code scope="rowgroup"} set on the resulting <code>&lt;th&gt;</code>.
     * Use it for the cell that names a group of consecutive rows, such as
     * "Terrestrial planets" in front of the four rows describing them, as
     * opposed to {@link #addRowHeaderCell(String)}, which labels one row.
     *
     * <p>
     * For example, {@code addRowGroupHeaderCell("Dwarf planets")} renders as:
     *
     * <pre>{@code
     * <th scope="rowgroup">Dwarf planets</th>
     * }</pre>
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} with {@code scope="rowgroup"}.
     */
    public TableHeaderCell addRowGroupHeaderCell(String text) {
        TableHeaderCell cell = addHeaderCell(text);
        cell.setScope(TableHeaderCell.Scope.ROWGROUP);
        return cell;
    }

    /**
     * Appends a header cell labelling the band of rows it heads, with
     * {@code scope="rowgroup"} set on the resulting <code>&lt;th&gt;</code> and
     * its text content following the given signal. Set the span on the returned
     * cell with {@link TableCell#setRowspan(int)}.
     * <p>
     * The cell follows the signal for as long as it is attached, so this is the
     * factory to use for a header whose label is reactive rather than fixed.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @return the new {@code <th>} with {@code scope="rowgroup"}.
     */
    public TableHeaderCell addRowGroupHeaderCell(Signal<String> textSignal) {
        TableHeaderCell cell = addHeaderCell(textSignal);
        cell.setScope(TableHeaderCell.Scope.ROWGROUP);
        return cell;
    }

    /**
     * Appends a header cell holding the given content and labelling the band of
     * rows it heads, with {@code scope="rowgroup"} set on the resulting
     * <code>&lt;th&gt;</code>. This is the content-taking form of
     * {@link #addRowGroupHeaderCell(String)}; set the span on the returned cell
     * with {@link TableCell#setRowspan(int)}, since a varargs parameter has to
     * come last and so cannot be combined with one in a single call.
     * <p>
     * For example, {@code addRowGroupHeaderCell(new Span("Gas giants"))}
     * renders as:
     *
     * <pre>{@code
     * <th scope="rowgroup"><span>Gas giants</span></th>
     * }</pre>
     *
     * @param content
     *            the content of the cell.
     * @return the new {@code <th>} with {@code scope="rowgroup"}.
     */
    public TableHeaderCell addRowGroupHeaderCell(Component... content) {
        TableHeaderCell cell = addHeaderCell(content);
        cell.setScope(TableHeaderCell.Scope.ROWGROUP);
        return cell;
    }

    /**
     * Appends a header cell labelling the band of rows it heads, with
     * {@code scope="rowgroup"} and the given {@code rowspan} set on the
     * resulting <code>&lt;th&gt;</code>, so that the cell reaches over the rows
     * it names.
     *
     * <p>
     * For example, {@code addRowGroupHeaderCell("Terrestrial planets", 4)}
     * renders as:
     *
     * <pre>{@code
     * <th scope="rowgroup" rowspan="4">Terrestrial planets</th>
     * }</pre>
     *
     * @param text
     *            the text content.
     * @param rowspan
     *            the number of rows the header covers, or {@code 0} to cover
     *            the rest of the row group.
     * @return the new {@code <th>} with {@code scope="rowgroup"}.
     * @throws IllegalArgumentException
     *             if {@code rowspan} is negative.
     */
    public TableHeaderCell addRowGroupHeaderCell(String text, int rowspan) {
        TableHeaderCell cell = new TableHeaderCell(text);
        cell.setScope(TableHeaderCell.Scope.ROWGROUP);
        // Set the span before attaching, so that a rejected one leaves the row
        // as it was
        cell.setRowspan(rowspan);
        return append(cell);
    }

    /**
     * Appends a header cell labelling the band of columns it heads, with
     * {@code scope="colgroup"} set on the resulting <code>&lt;th&gt;</code>.
     * This is the counterpart of {@link #addRowGroupHeaderCell(String)}, for a
     * cell that names a group of consecutive columns rather than a single one.
     *
     * <p>
     * For example, {@code addColumnGroupHeaderCell("Measurements")} renders as:
     *
     * <pre>{@code
     * <th scope="colgroup">Measurements</th>
     * }</pre>
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} with {@code scope="colgroup"}.
     */
    public TableHeaderCell addColumnGroupHeaderCell(String text) {
        TableHeaderCell cell = addHeaderCell(text);
        cell.setScope(TableHeaderCell.Scope.COLGROUP);
        return cell;
    }

    /**
     * Appends a header cell labelling the band of columns it heads, with
     * {@code scope="colgroup"} set on the resulting <code>&lt;th&gt;</code> and
     * its text content following the given signal. Set the span on the returned
     * cell with {@link TableCell#setColspan(int)}.
     * <p>
     * The cell follows the signal for as long as it is attached, so this is the
     * factory to use for a header whose label is reactive rather than fixed.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @return the new {@code <th>} with {@code scope="colgroup"}.
     */
    public TableHeaderCell addColumnGroupHeaderCell(Signal<String> textSignal) {
        TableHeaderCell cell = addHeaderCell(textSignal);
        cell.setScope(TableHeaderCell.Scope.COLGROUP);
        return cell;
    }

    /**
     * Appends a header cell holding the given content and labelling the band of
     * columns it heads, with {@code scope="colgroup"} set on the resulting
     * <code>&lt;th&gt;</code>. This is the content-taking form of
     * {@link #addColumnGroupHeaderCell(String)}; set the span on the returned
     * cell with {@link TableCell#setColspan(int)}, since a varargs parameter
     * has to come last and so cannot be combined with one in a single call.
     * <p>
     * For example, {@code addColumnGroupHeaderCell(new Span("Measurements"))}
     * renders as:
     *
     * <pre>{@code
     * <th scope="colgroup"><span>Measurements</span></th>
     * }</pre>
     *
     * @param content
     *            the content of the cell.
     * @return the new {@code <th>} with {@code scope="colgroup"}.
     */
    public TableHeaderCell addColumnGroupHeaderCell(Component... content) {
        TableHeaderCell cell = addHeaderCell(content);
        cell.setScope(TableHeaderCell.Scope.COLGROUP);
        return cell;
    }

    /**
     * Appends a header cell labelling the band of columns it heads, with
     * {@code scope="colgroup"} and the given {@code colspan} set on the
     * resulting <code>&lt;th&gt;</code>, so that the cell reaches over the
     * columns it names.
     *
     * <p>
     * For example, {@code addColumnGroupHeaderCell("Measurements", 3)} renders
     * as:
     *
     * <pre>{@code
     * <th scope="colgroup" colspan="3">Measurements</th>
     * }</pre>
     *
     * @param text
     *            the text content.
     * @param colspan
     *            the number of columns the header covers.
     * @return the new {@code <th>} with {@code scope="colgroup"}.
     * @throws IllegalArgumentException
     *             if {@code colspan} is less than 1.
     */
    public TableHeaderCell addColumnGroupHeaderCell(String text, int colspan) {
        TableHeaderCell cell = new TableHeaderCell(text);
        cell.setScope(TableHeaderCell.Scope.COLGROUP);
        // Set the span before attaching, so that a rejected one leaves the row
        // as it was
        cell.setColspan(colspan);
        return append(cell);
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
     * Appends a new data cell whose text content follows the given signal.
     * <p>
     * The cell follows the signal for as long as it is attached, so this is the
     * factory to use for a value that is reactive rather than fixed.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @return the new {@code <td>}.
     */
    public TableDataCell addDataCell(Signal<String> textSignal) {
        return append(new TableDataCell(textSignal));
    }

    /**
     * Appends a new data cell holding the given content to this row, for a cell
     * that is more than plain text.
     *
     * @param content
     *            the content of the cell.
     * @return the new {@code <td>}.
     */
    public TableDataCell addDataCell(Component... content) {
        return append(new TableDataCell(content));
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
     * This is the typed counterpart of {@link #getChildren()}. For
     * kind-specific lists use {@link #getHeaderCells()} or
     * {@link #getDataCells()}.
     *
     * @return this row's cells.
     */
    public List<TableCell> getCells() {
        return cellsOfType(TableCell.class);
    }

    private <T extends Component> List<T> cellsOfType(Class<T> type) {
        return ComponentUtil.getChildrenOfType(this, type).toList();
    }

    private <T extends TableCell> T append(T cell) {
        add(cell);
        return cell;
    }

    private <T extends TableCell> T insert(T cell, int position) {
        addComponentAtIndex(position, cell);
        return cell;
    }
}
