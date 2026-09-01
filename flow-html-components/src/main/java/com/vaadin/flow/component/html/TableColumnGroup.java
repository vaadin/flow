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

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponentsOfType;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.signals.Signal;

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
 * attribute. This component enforces that: adding a column to a group that
 * carries a {@code span} throws, as does setting a {@code span} on a group that
 * already has columns. Call {@link #resetSpan()} or {@link #removeAll()} first
 * to switch a group from one mode to the other. {@code <colgroup>} elements
 * must be placed after the optional {@code <caption>} and before any
 * {@code <thead>}, {@code <tbody>}, {@code <tfoot>} or <code>&lt;tr&gt;</code>;
 * {@link Table} inserts them at the correct position automatically.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/colgroup">MDN:
 *      &lt;colgroup&gt; — The Table Column Group element</a>
 * @since 25.3
 */
@NullMarked
@Tag(Tag.COLGROUP)
public class TableColumnGroup extends HtmlComponent
        implements TableColumnSpan, HasComponentsOfType<TableColumn> {

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
        add(columns);
    }

    /**
     * List equivalent of {@link #TableColumnGroup(TableColumn...)}.
     *
     * @param columns
     *            the columns to add.
     */
    public TableColumnGroup(List<? extends TableColumn> columns) {
        super();
        add(columns.toArray(TableColumn[]::new));
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
     * Creates a new empty {@code <col>} and inserts it at the given position in
     * this group. This is the counterpart of {@link #addColumn()}, matching
     * {@code insertRow}, {@code insertHeaderCell} and {@code insertDataCell}
     * elsewhere in the family.
     *
     * @param position
     *            the position to insert the column at, between 0 and the number
     *            of columns in this group.
     * @return the new column.
     * @throws IllegalStateException
     *             if this group carries a {@code span}, which a
     *             {@code <colgroup>} may not do alongside {@code <col>}
     *             children. Call {@link #resetSpan()} first.
     */
    public TableColumn insertColumn(int position) {
        TableColumn column = new TableColumn();
        addComponentAtIndex(position, column);
        return column;
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

    @Override
    public void add(Collection<TableColumn> columns) {
        rejectColumnsWhileSpanIsSet();
        HasComponentsOfType.super.add(columns);
    }

    @Override
    public void addComponentAtIndex(int index, TableColumn column) {
        rejectColumnsWhileSpanIsSet();
        HasComponentsOfType.super.addComponentAtIndex(index, column);
    }

    @Override
    public void addComponentAsFirst(TableColumn column) {
        rejectColumnsWhileSpanIsSet();
        HasComponentsOfType.super.addComponentAsFirst(column);
    }

    @Override
    public void replace(TableColumn oldColumn, TableColumn newColumn) {
        rejectColumnsWhileSpanIsSet();
        HasComponentsOfType.super.replace(oldColumn, newColumn);
    }

    /**
     * Not supported. The columns of a {@code <colgroup>} are structure rather
     * than data, and a binding would attach them straight to the element,
     * bypassing the check that keeps {@code span} and {@code <col>} children
     * mutually exclusive. Bind the rows of a {@link TableBody} instead.
     *
     * @throws UnsupportedOperationException
     *             always.
     */
    @Override
    public <V extends @Nullable Object, S extends Signal<V>> void bindChildren(
            Signal<List<S>> list,
            SerializableFunction<S, TableColumn> childFactory) {
        throw new UnsupportedOperationException(
                "A <colgroup> does not support a children binding, because it "
                        + "would bypass the rule that span and <col> children "
                        + "are mutually exclusive.");
    }

    private void rejectColumnsWhileSpanIsSet() {
        if (hasSpan()) {
            throw new IllegalStateException(
                    "A <colgroup> carrying a span attribute may not have <col> "
                            + "children. Call resetSpan() first.");
        }
    }

    private TableColumn addColumn(TableColumn column) {
        add(column);
        return column;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A {@code <colgroup>} carrying a {@code span} may not also have
     * {@code <col>} children, so this refuses a group that already has some.
     *
     * @throws IllegalStateException
     *             if this group has {@code <col>} children. Call
     *             {@link #removeAll()} first.
     */
    @Override
    public void setSpan(int span) {
        if (getElement().getChildCount() > 0) {
            throw new IllegalStateException(
                    "A <colgroup> with <col> children may not carry a span "
                            + "attribute. Call removeAll() first.");
        }
        TableColumnSpan.super.setSpan(span);
    }
}
