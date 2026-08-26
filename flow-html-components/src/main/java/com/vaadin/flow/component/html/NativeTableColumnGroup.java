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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;colgroup&gt;</code> element — a group of
 * columns inside a {@link NativeTable}, used to apply attributes (most often a
 * class for CSS) to several columns at once. Only a limited subset of CSS
 * applies to column groups: {@code background}, {@code border} (with
 * {@code border-collapse: collapse}), {@code visibility: collapse} and
 * {@code width}.
 * <p>
 * Per the <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG
 * HTML specification</a>, a {@code <colgroup>} is used in one of two modes:
 * either it carries a {@code span} attribute and has no children, or it
 * contains zero or more {@code <col>} children and has no {@code span}
 * attribute. Manage its {@code <col>} children through the
 * {@link NativeTableColumn} operations below rather than the generic
 * {@link HtmlContainer#add(Component...)} inherited from {@link HtmlContainer}.
 * {@code <colgroup>} elements must be placed after the optional
 * {@code <caption>} and before any {@code <thead>}, {@code <tbody>},
 * {@code <tfoot>} or <code>&lt;tr&gt;</code>; the {@link NativeTable} inserts
 * them at the correct position automatically.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/colgroup">MDN:
 *      &lt;colgroup&gt; — The Table Column Group element</a>
 */
@Tag(Tag.COLGROUP)
public class NativeTableColumnGroup extends HtmlContainer {

    private static final String ATTRIBUTE_SPAN = "span";

    /**
     * Creates a new empty column group.
     */
    public NativeTableColumnGroup() {
        super();
    }

    /**
     * Creates a new column group with the given columns appended.
     *
     * @param columns
     *            the columns to add.
     */
    public NativeTableColumnGroup(NativeTableColumn... columns) {
        super(columns);
    }

    /**
     * List equivalent of {@link #NativeTableColumnGroup(NativeTableColumn...)}.
     *
     * @param columns
     *            the columns to add.
     */
    public NativeTableColumnGroup(List<? extends NativeTableColumn> columns) {
        super(columns.toArray(NativeTableColumn[]::new));
    }

    /**
     * Appends a new empty {@code <col>} child to this group.
     *
     * @return the new column.
     */
    public NativeTableColumn addColumn() {
        NativeTableColumn column = new NativeTableColumn();
        add(column);
        return column;
    }

    /**
     * Appends a new {@code <col>} child with the given span to this group.
     *
     * @param span
     *            the number of columns to span.
     * @return the new column.
     */
    public NativeTableColumn addColumn(int span) {
        NativeTableColumn column = new NativeTableColumn(span);
        add(column);
        return column;
    }

    /**
     * Appends the given columns to this group.
     *
     * @param columns
     *            the columns to add.
     */
    public void addColumns(NativeTableColumn... columns) {
        addColumns(Arrays.asList(columns));
    }

    /**
     * List equivalent of {@link #addColumns(NativeTableColumn...)}.
     *
     * @param columns
     *            the columns to add.
     */
    public void addColumns(List<? extends NativeTableColumn> columns) {
        add(columns.toArray(NativeTableColumn[]::new));
    }

    /**
     * Returns the columns inside this group.
     *
     * @return the list of {@code <col>} children.
     */
    public List<NativeTableColumn> getColumns() {
        return ComponentUtil.getChildrenOfType(this, NativeTableColumn.class)
                .collect(Collectors.toList());
    }

    /**
     * Removes a column from this group.
     *
     * @param column
     *            the column to remove.
     */
    public void removeColumn(NativeTableColumn column) {
        remove(column);
    }

    /**
     * Removes all columns from this group.
     */
    public void removeAllColumns() {
        removeAll();
    }

    /**
     * Sets the {@code span} attribute — how many consecutive columns this group
     * covers when used without {@link NativeTableColumn} children. Per the HTML
     * specification, {@code span} is only valid on a {@code <colgroup>} that
     * has no {@code <col>} children. The default is {@code 1}.
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
