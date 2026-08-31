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

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponentsOfType;

/**
 * A container of <code>&lt;tr&gt;</code> elements, implemented by
 * {@link TableHead}, {@link TableBody} and {@link TableFoot}.
 * <p>
 * The WHATWG HTML specification allows nothing but rows inside those three
 * elements, which is what {@link HasComponentsOfType} expresses: the standard
 * {@code add}, {@code remove}, {@code replace} and
 * {@link HasComponentsOfType#bindChildren(com.vaadin.flow.signals.Signal, com.vaadin.flow.function.SerializableFunction)
 * bindChildren} operations are all available, but only for {@link TableRow}, so
 * an unrelated component is rejected at compile time rather than producing
 * invalid markup. What this interface adds on top are the row factories, which
 * create a row and attach it in one call.
 * <p>
 * Implementers must be {@link Component} instances.
 *
 * @since 25.3
 */
@NullMarked
interface TableRowContainer extends HasComponentsOfType<TableRow> {

    /**
     * Returns the rows in this container, in document order. This is the typed
     * counterpart of {@link #getChildren()}.
     *
     * @return the rows in this container.
     */
    default List<TableRow> getRows() {
        return ComponentUtil.getChildrenOfType((Component) this, TableRow.class)
                .toList();
    }

    /**
     * Creates a new empty row and appends it to this container.
     *
     * @return the new row.
     */
    default TableRow addRow() {
        TableRow row = new TableRow();
        add(row);
        return row;
    }

    /**
     * Creates a new empty row and inserts it at the given position.
     *
     * @param position
     *            the position to insert the row at, between 0 and the number of
     *            children in this container.
     * @return the new row.
     */
    default TableRow insertRow(int position) {
        TableRow row = new TableRow();
        addComponentAtIndex(position, row);
        return row;
    }
}
