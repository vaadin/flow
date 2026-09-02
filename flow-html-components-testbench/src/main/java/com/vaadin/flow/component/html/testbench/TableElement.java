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

import java.util.ArrayList;
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

    /**
     * Returns how many columns wide this table is once {@code colspan} and
     * {@code rowspan} are resolved, which is what the widest row occupies on
     * screen rather than the largest number of cells any row writes. Together
     * with {@link #getAllRows()} it bounds a loop over
     * {@link #getCellCovering(int, int)}.
     *
     * @return the number of columns, or 0 for a table with no cells.
     */
    public int getColumnCount() {
        return cellGrid().stream().mapToInt(List::size).max().orElse(0);
    }

    /**
     * Lays the cells of this table out as a grid, resolving {@code colspan} and
     * {@code rowspan} so that each entry is the cell occupying that slot on
     * screen. A spanning cell appears in every slot it covers; a slot no cell
     * reaches is {@code null}, which a ragged table can produce.
     * <p>
     * Rows are walked as in {@link #getAllRows()}. A {@code rowspan} of 0
     * reaches to the end of the row group holding it, as the HTML specification
     * says, and one that overruns its row group is cut off there.
     */
    private List<List<TableCellElement>> cellGrid() {
        List<List<TableCellElement>> grid = new ArrayList<>();
        sections(THEAD, TBODY, TFOOT)
                .forEach(section -> addRowGroup(section.getRows(), grid));
        int width = grid.stream().mapToInt(List::size).max().orElse(0);
        grid.forEach(row -> pad(row, width));
        return grid;
    }

    /**
     * Returns the cell covering the given zero-based slot of this table as it
     * appears on screen, resolving {@code colspan} and {@code rowspan}. Where
     * {@link #getCell(int, int)} counts the cells a row writes, this counts the
     * positions a reader sees, so a cell spanning two rows is returned for both
     * of them.
     *
     * @param row
     *            the zero-based row of the slot.
     * @param column
     *            the zero-based column of the slot.
     * @return the cell covering that slot.
     * @throws NoSuchElementException
     *             if the table has no such slot, or no cell reaches it.
     * @see #getColumnCount()
     */
    public TableCellElement getCellCovering(int row, int column) {
        List<List<TableCellElement>> grid = cellGrid();
        TableCellElement cell = row >= 0 && row < grid.size() && column >= 0
                && column < grid.get(row).size() ? grid.get(row).get(column)
                        : null;
        if (cell == null) {
            throw new NoSuchElementException("No cell covers row " + row
                    + " and column " + column + " of this table");
        }
        return cell;
    }

    /**
     * Lays the rows of one row group into the grid, starting below whatever is
     * already there. A {@code rowspan} is confined to its own row group, which
     * is what lets a span of 0 mean "to the end of this group".
     */
    private static void addRowGroup(List<TableRowElement> rows,
            List<List<TableCellElement>> grid) {
        int offset = grid.size();
        for (int i = 0; i < rows.size(); i++) {
            grid.add(new ArrayList<>());
        }
        for (int r = 0; r < rows.size(); r++) {
            List<TableCellElement> gridRow = grid.get(offset + r);
            int column = 0;
            for (TableCellElement cell : rows.get(r).getCells()) {
                while (column < gridRow.size() && gridRow.get(column) != null) {
                    column++;
                }
                int colspan = span(cell, "colSpan");
                int rowspan = span(cell, "rowSpan");
                // 0 means "to the end of the row group", and anything longer
                // than the group is cut off there
                int rows0 = rows.size() - r;
                rowspan = rowspan == 0 ? rows0 : Math.min(rowspan, rows0);
                for (int dr = 0; dr < rowspan; dr++) {
                    List<TableCellElement> target = grid.get(offset + r + dr);
                    for (int dc = 0; dc < colspan; dc++) {
                        set(target, column + dc, cell);
                    }
                }
                column += colspan;
            }
        }
    }

    /**
     * Reads a span through its DOM property rather than its attribute, so that
     * the browser has already applied the rules: an absent, negative or
     * unparseable span reads as 1, {@code colSpan} of 0 reads as 1 because that
     * value was dropped from HTML, and only {@code rowSpan} keeps a meaningful
     * 0. Reading the raw attribute would leave those to us and let a bogus
     * value place the cell somewhere the browser does not.
     */
    private static int span(TableCellElement cell, String property) {
        String value = cell.getDomProperty(property);
        return value == null ? 1 : Integer.parseInt(value);
    }

    private static void set(List<TableCellElement> row, int index,
            TableCellElement cell) {
        pad(row, index + 1);
        row.set(index, cell);
    }

    private static void pad(List<TableCellElement> row, int width) {
        while (row.size() < width) {
            row.add(null);
        }
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
