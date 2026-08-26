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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;

/**
 * A container of <code>&lt;tr&gt;</code> elements.
 *
 * @since 24.4
 */
interface NativeTableRowContainer extends HasComponents {

    /**
     * Returns a list of all the rows.
     *
     * @return all the rows in the container.
     */
    default List<NativeTableRow> getRows() {
        return getChildren().filter(c -> c instanceof NativeTableRow)
                .map(c -> (NativeTableRow) c).collect(Collectors.toList());
    }

    /**
     * Appends a list of rows to the container.
     *
     * @param rows
     *            the rows to append.
     */
    default void addRows(NativeTableRow... rows) {
        add(rows);
    }

    /**
     * List equivalent of {@link #addRows(NativeTableRow...)}.
     *
     * @param rows
     *            the rows to append.
     */
    default void addRows(List<? extends NativeTableRow> rows) {
        add(rows.toArray(NativeTableRow[]::new));
    }

    /**
     * Create and append a row to the end of the container.
     *
     * @return the new row.
     */
    default NativeTableRow addRow() {
        NativeTableRow row = new NativeTableRow();
        add(row);
        return row;
    }

    /**
     * Create and insert a row at a given position.
     *
     * @param position
     *            a value greater than 0 and less than the container's size.
     * @return the new row.
     */
    default NativeTableRow insertRow(int position) {
        NativeTableRow row = new NativeTableRow();
        addComponentAtIndex(position, row);
        return row;
    }

    /**
     * Remove a list of rows from the container.
     *
     * @param rows
     *            the rows to remove. If a component in the list is not a child
     *            of the container, it will throw an exception.
     */
    default void removeRows(NativeTableRow... rows) {
        remove(rows);
    }

    /**
     * List equivalent of {@link #removeRows(NativeTableRow...)}.
     *
     * @param rows
     *            the rows to remove.
     */
    default void removeRows(List<? extends NativeTableRow> rows) {
        remove(rows.toArray(NativeTableRow[]::new));
    }

    /**
     * Remove all the rows in the container.
     */
    default void removeAllRows() {
        removeAll();
    }

    /**
     * Replaces the row at a given position with a new one. If both rows exist
     * within the container, they swap positions.
     *
     * @param index
     *            the index of the row to replace.
     * @param row
     *            the new row to insert at the position of the old row.
     */
    default void replaceRow(int index, NativeTableRow row) {
        Component oldRow = getComponentAt(index);
        replace(oldRow, row);
    }

}
