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
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tr&gt;</code> element.
 * <p>
 * Per the
 * <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG HTML
 * specification</a>, a {@code <tr>} may only contain {@code <td>} or
 * {@code <th>} elements, so build its content through the cell operations
 * below rather than the generic {@link HtmlContainer#add(Component...)}
 * inherited from {@link HtmlContainer}.
 * <p>
 * The {@link #NativeTableRow(Component...) constructor} and {@link #addCells} accept
 * any {@link Component}: {@link AbstractNativeTableCell} subclasses ({@link NativeTableCell},
 * {@link NativeTableHeaderCell}) are placed as-is, and any other component is
 * automatically wrapped in a new {@link NativeTableCell}.
 *
 * @since 24.5
 */
@Tag(Tag.TR)
public class NativeTableRow extends HtmlContainer
        implements ClickNotifier<NativeTableRow> {

    /**
     * Creates a new empty table row component.
     */
    public NativeTableRow() {
        super();
    }

    /**
     * Creates a new table row with the given children. Any {@link AbstractNativeTableCell}
     * argument ({@link NativeTableCell} or {@link NativeTableHeaderCell}) is added
     * as-is; any other component is wrapped in a new {@link NativeTableCell}
     * — convenient for building rows from arbitrary content without the
     * boilerplate of explicit {@code new NativeTableCell(...)} wrappers.
     *
     * @param components
     *            the cells (used as-is) or other components (wrapped in
     *            {@code <td>}) to place in this row.
     */
    public NativeTableRow(Component... components) {
        this(Arrays.asList(components));
    }

    /**
     * List equivalent of {@link #NativeTableRow(Component...)}.
     *
     * @param components
     *            the cells or wrap-target components for this row.
     */
    public NativeTableRow(List<? extends Component> components) {
        super();
        appendAsCells(components);
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
     * shortcut for the common pattern of using a leading {@code <th>} as a
     * row label, which assistive technologies announce as the header for
     * the data cells in the same row.
     *
     * @param text
     *            the text content.
     * @return the new {@code <th>} element with {@code scope="row"}.
     */
    public NativeTableHeaderCell addRowHeaderCell(String text) {
        NativeTableHeaderCell cell = new NativeTableHeaderCell(text);
        cell.setScope(NativeTableHeaderCell.Scope.ROW);
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
     * Insert a new data cell into a given position.
     *
     * @param position
     *            the position into which the data cell must be added.
     * @return the new data cell.
     */
    public NativeTableCell insertDataCell(int position) {
        NativeTableCell tableCell = new NativeTableCell();
        addComponentAtIndex(position, tableCell);
        return tableCell;
    }

    /**
     * Returns a list of all header cells in this row.
     *
     * @return A list of all header cells in this row.
     */
    public List<NativeTableHeaderCell> getHeaderCells() {
        return ComponentUtil.getChildrenOfType(this, NativeTableHeaderCell.class)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all data cells in this row.
     *
     * @return A list of all data cells in this row.
     */
    public List<NativeTableCell> getDataCells() {
        return ComponentUtil.getChildrenOfType(this, NativeTableCell.class)
                .collect(Collectors.toList());
    }

    /**
     * Returns all cells in this row, in document order — both
     * {@link NativeTableCell} and {@link NativeTableHeaderCell} entries combined. For
     * kind-specific lists use {@link #getDataCells()} or
     * {@link #getHeaderCells()}; index into any of these lists with
     * {@code .get(i)}.
     *
     * @return a list of all cells in this row.
     */
    public List<AbstractNativeTableCell> getCells() {
        return ComponentUtil.getChildrenOfType(this, AbstractNativeTableCell.class)
                .collect(Collectors.toList());
    }

    /**
     * Removes a cell from this row.
     *
     * @param cell
     *            the cell to remove.
     */
    public void removeCell(AbstractNativeTableCell cell) {
        remove(cell);
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
        for (String text : cellTexts) {
            addHeaderCell(text);
        }
        return this;
    }

    /**
     * Appends children to this row. Any {@link AbstractNativeTableCell} argument
     * ({@link NativeTableCell} or {@link NativeTableHeaderCell}) is added as-is;
     * any other component is wrapped in a new {@link NativeTableCell}.
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
        appendAsCells(components);
        return this;
    }

    private void appendAsCells(Iterable<? extends Component> components) {
        for (Component c : components) {
            AbstractNativeTableCell cell = (c instanceof AbstractNativeTableCell tc) ? tc
                    : new NativeTableCell(c);
            add(cell);
        }
    }

}
