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
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a <code>&lt;table&gt;</code> element.
 *
 * @since 25.3
 */
@Element("table")
public class TableElement extends TestBenchElement {

    /**
     * Returns every row of this table, walking its sections in document order.
     * For a table rendered by {@code Table} that is the
     * <code>&lt;thead&gt;</code> rows, then the rows of each
     * <code>&lt;tbody&gt;</code>, then the <code>&lt;tfoot&gt;</code> rows; on
     * a page that writes the sections in some other order, the result follows
     * the markup rather than that order.
     *
     * @return the rows of this table.
     */
    public List<TableRowElement> getRows() {
        return rowsOf(sections("thead", "tbody", "tfoot"));
    }

    /**
     * Returns the row at the given position among all the rows of this table.
     *
     * @param index
     *            the position of the row.
     * @return the row at that position.
     */
    public TableRowElement getRow(int index) {
        return getRows().get(index);
    }

    /**
     * Returns the cell at the given row and column of this table, counting
     * across all its rows.
     *
     * @param row
     *            the position of the row.
     * @param column
     *            the position of the cell within that row.
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
        return rowsOf(sections("thead"));
    }

    /**
     * Returns the rows of this table's <code>&lt;tbody&gt;</code> elements, in
     * document order. Use {@link #getBodies()} to keep them grouped by body.
     *
     * @return the body rows, or an empty list if the table has no
     *         <code>&lt;tbody&gt;</code>.
     */
    public List<TableRowElement> getBodyRows() {
        return rowsOf(sections("tbody"));
    }

    /**
     * Returns the rows of this table's <code>&lt;tfoot&gt;</code>.
     *
     * @return the footer rows, or an empty list if the table has no
     *         <code>&lt;tfoot&gt;</code>.
     */
    public List<TableRowElement> getFooterRows() {
        return rowsOf(sections("tfoot"));
    }

    /**
     * Returns the <code>&lt;tbody&gt;</code> elements of this table, in
     * document order. A table may have several, each holding its own rows.
     *
     * @return the table bodies.
     */
    public List<TableBodyElement> getBodies() {
        return childrenNamed("tbody").stream()
                .map(child -> child.wrap(TableBodyElement.class)).toList();
    }

    /**
     * Returns this table's <code>&lt;thead&gt;</code>.
     *
     * @return the header section, or an empty optional if the table has none.
     */
    public Optional<TableHeadElement> getHead() {
        return childrenNamed("thead").stream().findFirst()
                .map(child -> child.wrap(TableHeadElement.class));
    }

    /**
     * Returns this table's <code>&lt;tfoot&gt;</code>.
     *
     * @return the footer section, or an empty optional if the table has none.
     */
    public Optional<TableFootElement> getFoot() {
        return childrenNamed("tfoot").stream().findFirst()
                .map(child -> child.wrap(TableFootElement.class));
    }

    /**
     * Returns this table's caption.
     *
     * @return the <code>&lt;caption&gt;</code>, or an empty optional if the
     *         table has none.
     */
    public Optional<TableCaptionElement> getCaption() {
        return childrenNamed("caption").stream().findFirst()
                .map(child -> child.wrap(TableCaptionElement.class));
    }

    /**
     * Returns the column groups of this table, in document order.
     *
     * @return the table's <code>&lt;colgroup&gt;</code> elements.
     */
    public List<TableColumnGroupElement> getColumnGroups() {
        return childrenNamed("colgroup").stream()
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
