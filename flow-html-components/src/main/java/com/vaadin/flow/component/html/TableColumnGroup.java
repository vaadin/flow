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

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;colgroup&gt;</code> element — a group of
 * columns inside a {@link Table}, used to apply attributes (most often a class
 * for CSS) to several columns at once. Only a limited subset of CSS applies to
 * column groups: {@code background}, {@code border} (with
 * {@code border-collapse: collapse}), {@code visibility: collapse} and
 * {@code width}.
 * <p>
 * Per the <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG
 * HTML specification</a>, a {@code <colgroup>} is used in one of two modes:
 * either it carries a {@code span} attribute and has no children, or it
 * contains zero or more {@code <col>} children and has no {@code span}
 * attribute. {@code <colgroup>} elements must be placed after the optional
 * {@code <caption>} and before any {@code <thead>}, {@code <tbody>},
 * {@code <tfoot>} or <code>&lt;tr&gt;</code>; {@link Table} inserts them at the
 * correct position automatically.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/colgroup">MDN:
 *      &lt;colgroup&gt; — The Table Column Group element</a>
 * @since 25.3
 */
@NullMarked
@Tag(Tag.COLGROUP)
public class TableColumnGroup extends HtmlComponent implements TableColumnSpan {

    /**
     * Creates a new empty column group.
     */
    public TableColumnGroup() {
        super();
    }

    /**
     * Creates a new column group with the given columns appended.
     *
     * @param columns
     *            the columns to add.
     */
    public TableColumnGroup(TableColumn... columns) {
        super();
        addColumns(columns);
    }

    /**
     * List equivalent of {@link #TableColumnGroup(TableColumn...)}.
     *
     * @param columns
     *            the columns to add.
     */
    public TableColumnGroup(List<? extends TableColumn> columns) {
        super();
        addColumns(columns);
    }

    /**
     * Appends a new empty {@code <col>} child to this group.
     *
     * @return the new column.
     */
    public TableColumn addColumn() {
        return addColumn(new TableColumn());
    }

    /**
     * Appends a new {@code <col>} child with the given span to this group.
     *
     * @param span
     *            the number of columns to span.
     * @return the new column.
     */
    public TableColumn addColumn(int span) {
        return addColumn(new TableColumn(span));
    }

    /**
     * Appends the given columns to this group.
     *
     * @param columns
     *            the columns to add.
     */
    public void addColumns(TableColumn... columns) {
        addColumns(Arrays.asList(columns));
    }

    /**
     * List equivalent of {@link #addColumns(TableColumn...)}.
     *
     * @param columns
     *            the columns to add.
     */
    public void addColumns(List<? extends TableColumn> columns) {
        columns.forEach(this::addColumn);
    }

    /**
     * Returns the columns inside this group.
     *
     * @return the list of {@code <col>} children.
     */
    public List<TableColumn> getColumns() {
        return ComponentUtil.getChildrenOfType(this, TableColumn.class)
                .toList();
    }

    /**
     * Removes a column from this group.
     *
     * @param column
     *            the column to remove.
     */
    public void removeColumn(TableColumn column) {
        getElement().removeChild(column.getElement());
    }

    /**
     * Removes all columns from this group.
     */
    public void removeAllColumns() {
        getElement().removeAllChildren();
    }

    private TableColumn addColumn(TableColumn column) {
        getElement().appendChild(column.getElement());
        return column;
    }
}
