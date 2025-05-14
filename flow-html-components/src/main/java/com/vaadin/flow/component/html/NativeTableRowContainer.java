/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasOrderedComponents;

/**
 * A container of <code>&lt;tr&gt;</code> elements.
 *
 * @since 24.4
 */
interface NativeTableRowContainer extends HasOrderedComponents {

    /**
     * Get the index of a given row.
     *
     * @param row
     *            the row to get the index of.
     * @return the index of the row.
     */
    default int getRowIndex(NativeTableRow row) {
        return indexOf(row);
    }

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
     * Returns the row at the given index.
     *
     * @param index
     *            the index of the row. Must be greater than 0 and less than the
     *            size of the container.
     * @return the row at position {@code index}.
     */
    default Optional<NativeTableRow> getRow(int index) {
        return getChildren().filter(c -> c instanceof NativeTableRow)
                .map(c -> (NativeTableRow) c).skip(index).findFirst();
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
     * Remove the row at the given index.
     *
     * @param index
     *            the position of the row to remove.
     */
    default void removeRow(int index) {
        getRow(index).ifPresent(this::remove);

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

    /**
     * Returns the number of rows in the container.
     *
     * @return the row count.
     */
    default long getRowCount() {
        return getChildren().filter(c -> c instanceof NativeTableRow).count();
    }

}
