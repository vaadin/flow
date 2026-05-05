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

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;colgroup&gt;</code> element which defines
 * a group of columns within a {@link Table}.
 * <p>
 * Per the <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG
 * HTML specification</a>, a {@code <colgroup>} either has a {@code span}
 * attribute (and no {@code <col>} children), or contains zero or more
 * {@code <col>} children (and no {@code span} attribute). This component
 * therefore extends {@link HtmlComponent} (rather than
 * {@link com.vaadin.flow.component.HtmlContainer}) and exposes only operations
 * for managing {@link TableColumn} children plus the {@code span} attribute.
 *
 * @since 25.2
 */
@Tag(Tag.COLGROUP)
public class TableColumnGroup extends HtmlComponent {

    private static final String ATTRIBUTE_SPAN = "span";

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
     * Appends a new empty {@code <col>} child to this group.
     *
     * @return the new column.
     */
    public TableColumn addColumn() {
        TableColumn column = new TableColumn();
        getElement().appendChild(column.getElement());
        return column;
    }

    /**
     * Appends a new {@code <col>} child with the given span to this group.
     *
     * @param span
     *            the number of columns to span.
     * @return the new column.
     */
    public TableColumn addColumn(int span) {
        TableColumn column = new TableColumn(span);
        getElement().appendChild(column.getElement());
        return column;
    }

    /**
     * Appends the given columns to this group.
     *
     * @param columns
     *            the columns to add.
     */
    public void addColumns(TableColumn... columns) {
        for (TableColumn column : columns) {
            getElement().appendChild(column.getElement());
        }
    }

    /**
     * Returns the columns inside this group.
     *
     * @return the list of {@code <col>} children.
     */
    public List<TableColumn> getColumns() {
        return getChildren().filter(c -> c instanceof TableColumn)
                .map(c -> (TableColumn) c).collect(Collectors.toList());
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

    /**
     * Sets the {@code span} attribute on this column group. Per the HTML
     * specification, the {@code span} attribute is only valid when the group
     * has no {@code <col>} children.
     *
     * @param span
     *            a positive integer.
     */
    public void setSpan(int span) {
        if (span < 1) {
            throw new IllegalArgumentException(
                    "span must be a positive integer value");
        }
        getElement().setAttribute(ATTRIBUTE_SPAN, String.valueOf(span));
    }

    /**
     * Returns the value of the {@code span} attribute.
     *
     * @return the current span. Default is 1.
     */
    public int getSpan() {
        String span = getElement().getAttribute(ATTRIBUTE_SPAN);
        if (span == null) {
            span = "1";
        }
        return Integer.parseInt(span);
    }

    /**
     * Removes the {@code span} attribute, restoring the default value of 1.
     */
    public void resetSpan() {
        getElement().removeAttribute(ATTRIBUTE_SPAN);
    }
}
