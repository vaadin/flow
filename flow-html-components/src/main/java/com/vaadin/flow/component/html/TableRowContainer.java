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
import com.vaadin.flow.component.HasElement;

/**
 * A container of <code>&lt;tr&gt;</code> elements, implemented by
 * {@link TableHead}, {@link TableBody} and {@link TableFoot}.
 * <p>
 * The WHATWG HTML specification allows nothing but rows inside those three
 * elements, so the operations here are the only ones offered: there is
 * deliberately no generic {@code add(Component)}.
 * <p>
 * Implementers must be {@link Component} instances.
 *
 * @since 25.3
 */
@NullMarked
interface TableRowContainer extends HasElement {

    /**
     * Returns the rows in this container, in document order.
     *
     * @return the rows in this container.
     */
    default List<TableRow> getRows() {
        return ComponentUtil.getChildrenOfType((Component) this, TableRow.class)
                .toList();
    }

    /**
     * Appends a new empty row to this container.
     *
     * @return the new row.
     */
    default TableRow addRow() {
        TableRow row = new TableRow();
        getElement().appendChild(row.getElement());
        return row;
    }

    /**
     * Appends the given rows to this container.
     *
     * @param rows
     *            the rows to append.
     */
    default void addRows(TableRow... rows) {
        for (TableRow row : rows) {
            getElement().appendChild(row.getElement());
        }
    }

    /**
     * Inserts a new empty row at the given position.
     *
     * @param position
     *            the position to insert the row at, between 0 and the number of
     *            children in this container.
     * @return the new row.
     */
    default TableRow insertRow(int position) {
        TableRow row = new TableRow();
        getElement().insertChild(position, row.getElement());
        return row;
    }

    /**
     * Replaces the row at the given position with the given one.
     *
     * @param position
     *            the position of the row to replace.
     * @param row
     *            the row to put there instead.
     */
    default void replaceRow(int position, TableRow row) {
        getElement().setChild(position, row.getElement());
    }

    /**
     * Removes the given rows from this container.
     *
     * @param rows
     *            the rows to remove.
     */
    default void removeRows(TableRow... rows) {
        for (TableRow row : rows) {
            getElement().removeChild(row.getElement());
        }
    }

    /**
     * Removes every row from this container.
     */
    default void removeAllRows() {
        getElement().removeAllChildren();
    }
}
