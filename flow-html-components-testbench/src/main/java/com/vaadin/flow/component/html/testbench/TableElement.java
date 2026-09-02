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
package com.vaadin.flow.component.html.testbench;

import java.util.List;
import java.util.stream.Stream;

import org.openqa.selenium.NoSuchElementException;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a <code>&lt;table&gt;</code> element.
 *
 * @since 25.3
 */
@Element("table")
public class TableElement extends TestBenchElement {

    private static final String CAPTION = "caption";
    private static final String COLGROUP = "colgroup";
    private static final String THEAD = "thead";
    private static final String TBODY = "tbody";
    private static final String TFOOT = "tfoot";

    /**
     * Returns every row of this table, walking its sections in document order.
     * For a table rendered by {@code Table} that is the
     * <code>&lt;thead&gt;</code> rows, then the rows of each
     * <code>&lt;tbody&gt;</code>, then the <code>&lt;tfoot&gt;</code> rows; on
     * a page that writes the sections in some other order, the result follows
     * the markup rather than that order.
     * <p>
     * The name matches {@code Table.getAllRows()}: this is the only row
     * accessor here that spans every kind of section. {@link #getHeaderRows()},
     * {@link #getBodyRows()} and {@link #getFooterRows()} each stay within one
     * kind — though a table may have several <code>&lt;tbody&gt;</code>
     * elements, so {@code getBodyRows} flattens those — and
     * {@link TableSectionElement#getRows()} returns the rows of a single
     * element.
     *
     * @return all the rows of this table.
     */
    public List<TableRowElement> getAllRows() {
        return rowsOf(sections(THEAD, TBODY, TFOOT));
    }

    /**
     * Returns the row at the given zero-based position in
     * {@link #getAllRows()}, so the index runs across the sections rather than
     * within one.
     *
     * @param index
     *            the zero-based position of the row.
     * @return the row at that position.
     */
    public TableRowElement getRow(int index) {
        return getAllRows().get(index);
    }

    /**
     * Returns the cell at the given zero-based row and column of this table.
     * Both indices are zero-based, as in {@code GridElement.getCell}. The row
     * index runs across the sections, as in {@link #getAllRows()}; equivalent
     * to {@code getRow(row).getCells().get(column)}.
     * <p>
     * The resemblance to {@code GridElement.getCell} stops at the numbering,
     * though. A grid is a strict rectangle whose column index picks a column
     * rather than a position, so there every row has one cell per column and
     * the two always agree. A table does not have to be rectangular.
     * <p>
     * <b>Both indices count elements as they are written, not grid
     * positions.</b> A cell carrying a {@code colspan} or {@code rowspan}
     * occupies one position in the row it is written in and none at all in the
     * rows or columns it reaches into, so on a table with spans these indices
     * drift away from what is on screen. Given:
     *
     * <pre>{@code
     * <tr><th rowspan="2">Helsinki</th><td>Travel</td></tr>
     * <tr><td>Design</td></tr>
     * }</pre>
     *
     * {@code getCell(0, 0)} is the {@code 
     * 
    <th>} and {@code getCell(0, 1)} is "Travel", while {@code getCell(1, 0)} is
     * "Design" — not the {@code 
     * 
    <th>} that visually covers that position, and there is no index at which the
     * second row yields it. To assert on a spanned cell, reach it through the
     * row that writes it and check its {@code rowspan} or {@code colspan}
     * attribute.
     *
     * @param row
     *            the zero-based position of the row in {@link #getAllRows()}.
     * @param column
     *            the zero-based position of the cell within that row, counting
     *            only the cells that row writes.
     * @return the cell at that position.
     */
    public TableCellElement getCell(int row, int column) {
        return getRow(row).getCell(column);
    }

    /**
     * Returns the rows of this table's <code>&lt;thead&gt;</code>.
     *
     * @return the header rows, or an empty list if the table has no
     *         <code>&lt;thead&gt;</code>.
     */
    public List<TableRowElement> getHeaderRows() {
        return rowsOf(sections(THEAD));
    }

    /**
     * Returns the rows of this table's <code>&lt;tbody&gt;</code> elements, in
     * document order. Use {@link #getBodies()} to keep them grouped by body.
     *
     * @return the body rows, or an empty list if the table has no
     *         <code>&lt;tbody&gt;</code>.
     */
    public List<TableRowElement> getBodyRows() {
        return rowsOf(sections(TBODY));
    }

    /**
     * Returns the rows of this table's <code>&lt;tfoot&gt;</code>.
     *
     * @return the footer rows, or an empty list if the table has no
     *         <code>&lt;tfoot&gt;</code>.
     */
    public List<TableRowElement> getFooterRows() {
        return rowsOf(sections(TFOOT));
    }

    /**
     * Returns the <code>&lt;tbody&gt;</code> elements of this table, in
     * document order. A table may have several, each holding its own rows.
     *
     * @return the table bodies.
     */
    public List<TableBodyElement> getBodies() {
        return childrenNamed(TBODY).stream()
                .map(child -> child.wrap(TableBodyElement.class)).toList();
    }

    /**
     * Returns this table's <code>&lt;thead&gt;</code>.
     *
     * @return the header section.
     * @throws NoSuchElementException
     *             if the table has none. Ask {@link #hasHead()} first to check
     *             without failing; {@link #getHeaderRows()} is not the same
     *             question, since an empty {@code <thead>} has no rows.
     */
    public TableHeadElement getHead() {
        return childrenNamed(THEAD).stream().findFirst()
                .map(child -> child.wrap(TableHeadElement.class))
                .orElseThrow(() -> new NoSuchElementException(
                        "The table has no <thead>"));
    }

    /**
     * Reports whether this table has a <code>&lt;thead&gt;</code>, without
     * failing if it does not.
     *
     * @return {@code true} if the section is present.
     */
    public boolean hasHead() {
        return !childrenNamed(THEAD).isEmpty();
    }

    /**
     * Returns this table's <code>&lt;tfoot&gt;</code>.
     *
     * @return the footer section.
     * @throws NoSuchElementException
     *             if the table has none. Ask {@link #hasFoot()} first to check
     *             without failing; {@link #getFooterRows()} is not the same
     *             question, since an empty {@code <tfoot>} has no rows.
     */
    public TableFootElement getFoot() {
        return childrenNamed(TFOOT).stream().findFirst()
                .map(child -> child.wrap(TableFootElement.class))
                .orElseThrow(() -> new NoSuchElementException(
                        "The table has no <tfoot>"));
    }

    /**
     * Reports whether this table has a <code>&lt;tfoot&gt;</code>, without
     * failing if it does not.
     *
     * @return {@code true} if the section is present.
     */
    public boolean hasFoot() {
        return !childrenNamed(TFOOT).isEmpty();
    }

    /**
     * Returns this table's caption.
     *
     * @return the <code>&lt;caption&gt;</code>.
     * @throws NoSuchElementException
     *             if the table has none. Ask {@link #hasCaption()} first to
     *             check without failing.
     */
    public TableCaptionElement getCaption() {
        return childrenNamed(CAPTION).stream().findFirst()
                .map(child -> child.wrap(TableCaptionElement.class))
                .orElseThrow(() -> new NoSuchElementException(
                        "The table has no <caption>"));
    }

    /**
     * Reports whether this table has a <code>&lt;caption&gt;</code>, without
     * failing if it does not.
     *
     * @return {@code true} if the caption is present.
     */
    public boolean hasCaption() {
        return !childrenNamed(CAPTION).isEmpty();
    }

    /**
     * Returns the column groups of this table, in document order.
     *
     * @return the table's <code>&lt;colgroup&gt;</code> elements.
     */
    public List<TableColumnGroupElement> getColumnGroups() {
        return childrenNamed(COLGROUP).stream()
                .map(child -> child.wrap(TableColumnGroupElement.class))
                .toList();
    }

    private static List<TableRowElement> rowsOf(
            Stream<TableSectionElement> sections) {
        return sections.flatMap(section -> section.getRows().stream()).toList();
    }

    private Stream<TableSectionElement> sections(String... tagNames) {
        return childrenNamed(tagNames).stream()
                .map(child -> child.wrap(TableSectionElement.class));
    }

    /**
     * The caption, column groups and sections of this table are its direct
     * children; a descendant query would also pick up those of a table nested
     * inside a cell.
     */
    private List<TestBenchElement> childrenNamed(String... tagNames) {
        List<String> wanted = List.of(tagNames);
        return getChildren().stream()
                .filter(child -> wanted.stream().anyMatch(
                        tag -> tag.equalsIgnoreCase(child.getTagName())))
                .toList();
    }
}
