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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;

/**
 * A container of <code>&lt;tr&gt;</code> elements. Implemented by
 * {@link TableHead}, {@link TableBody} and {@link TableFoot}.
 * <p>
 * Only {@link TableRow} children are accepted, matching the WHATWG HTML
 * structural rules for {@code <thead>}, {@code <tbody>} and {@code <tfoot>}.
 * <p>
 * Implementers must also be {@link Component} instances.
 *
 * @since 25.2
 */
interface TableRowContainer extends HasElement {

    private Stream<Component> rowComponents() {
        if (this instanceof Component component) {
            return component.getChildren();
        }
        throw new UnsupportedOperationException(
                "TableRowContainer must be implemented by a Component");
    }

    /**
     * Get the index of a given row.
     *
     * @param row
     *            the row to get the index of.
     * @return the index of the row, or -1 if the row is not a child of this
     *         container.
     */
    default int getRowIndex(TableRow row) {
        return getElement().indexOfChild(row.getElement());
    }

    /**
     * Returns a list of all the rows.
     *
     * @return all the rows in the container.
     */
    default List<TableRow> getRows() {
        return rowComponents().filter(c -> c instanceof TableRow)
                .map(c -> (TableRow) c).collect(Collectors.toList());
    }

    /**
     * Returns the row at the given index.
     *
     * @param index
     *            the index of the row. Must be greater than or equal to 0 and
     *            less than the size of the container.
     * @return the row at position {@code index}.
     */
    default Optional<TableRow> getRow(int index) {
        return rowComponents().filter(c -> c instanceof TableRow)
                .map(c -> (TableRow) c).skip(index).findFirst();
    }

    /**
     * Appends a list of rows to the container.
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
     * Create and append a row to the end of the container.
     *
     * @return the new row.
     */
    default TableRow addRow() {
        TableRow row = new TableRow();
        getElement().appendChild(row.getElement());
        return row;
    }

    /**
     * Create and insert a row at a given position.
     *
     * @param position
     *            a value greater than or equal to 0 and less than or equal to
     *            the container's size.
     * @return the new row.
     */
    default TableRow insertRow(int position) {
        TableRow row = new TableRow();
        getElement().insertChild(position, row.getElement());
        return row;
    }

    /**
     * Remove a list of rows from the container.
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
     * Remove the row at the given index.
     *
     * @param index
     *            the position of the row to remove.
     */
    default void removeRow(int index) {
        getRow(index)
                .ifPresent(row -> getElement().removeChild(row.getElement()));
    }

    /**
     * Remove all the rows in the container.
     */
    default void removeAllRows() {
        getElement().removeAllChildren();
    }

    /**
     * Replaces the row at a given position with a new one.
     *
     * @param index
     *            the index of the row to replace.
     * @param row
     *            the new row to insert at the position of the old row.
     */
    default void replaceRow(int index, TableRow row) {
        getElement().setChild(index, row.getElement());
    }

    /**
     * Returns the number of rows in the container.
     *
     * @return the row count.
     */
    default long getRowCount() {
        return rowComponents().filter(c -> c instanceof TableRow).count();
    }

}
