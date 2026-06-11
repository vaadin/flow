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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;table&gt;</code> element — a
 * two-dimensional grid of cells with optional header, body and footer sections,
 * captioning and column-level styling.
 * <p>
 * Per the <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG
 * HTML specification</a>, a <code>&lt;table&gt;</code> may contain (in order):
 * an optional <code>&lt;caption&gt;</code>, zero or more
 * <code>&lt;colgroup&gt;</code> elements, an optional
 * <code>&lt;thead&gt;</code>, zero or more <code>&lt;tbody&gt;</code> elements,
 * and an optional <code>&lt;tfoot&gt;</code>. This component therefore extends
 * {@link HtmlComponent} (rather than
 * {@link com.vaadin.flow.component.HtmlContainer}) and exposes only the
 * structured operations required to build a valid table — child components are
 * inserted at the correct position automatically.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/table">MDN:
 *      &lt;table&gt; — The Table element</a>
 * @since 25.2
 */
@Tag(Tag.TABLE)
public class Table extends HtmlComponent implements ClickNotifier<Table> {

    private TableCaption caption;
    private final List<TableColumnGroup> columnGroups = new LinkedList<>();
    private TableHead head;
    private final List<TableBody> bodies = new LinkedList<>();
    private TableFoot foot;

    /**
     * Creates a new empty table.
     */
    public Table() {
        super();
    }

    /**
     * Return the table's caption component. Creates a new instance if no
     * caption is present.
     *
     * @return the table's caption.
     */
    public TableCaption getCaption() {
        if (caption == null) {
            caption = new TableCaption();
            getElement().insertChild(0, caption.getElement());
        }
        return caption;
    }

    /**
     * Returns the caption component if one has been set.
     *
     * @return an {@link Optional} containing the caption, or empty if none.
     */
    public Optional<TableCaption> findCaption() {
        return Optional.ofNullable(caption);
    }

    /**
     * Returns the caption text for this table, or an empty string if no caption
     * has been set.
     *
     * @return the table's caption text.
     */
    public String getCaptionText() {
        return caption == null ? "" : caption.getText();
    }

    /**
     * Sets the caption text for this table. Creates a caption element if none
     * exists.
     *
     * @param text
     *            the caption's text
     */
    public void setCaptionText(String text) {
        getCaption().setText(text);
    }

    /**
     * Appends the given components to this table's caption, creating it if none
     * exists yet. Useful for richer captions containing inline markup.
     *
     * @param components
     *            the components to append.
     * @return the caption.
     */
    public TableCaption addCaption(Component... components) {
        return addCaption(Arrays.asList(components));
    }

    /**
     * List equivalent of {@link #addCaption(Component...)}.
     *
     * @param components
     *            the components to append.
     * @return the caption.
     */
    public TableCaption addCaption(List<? extends Component> components) {
        TableCaption c = getCaption();
        c.add(components.toArray(Component[]::new));
        return c;
    }

    /**
     * Remove the caption from this table.
     */
    public void removeCaption() {
        if (caption != null) {
            getElement().removeChild(caption.getElement());
            caption = null;
        }
    }

    /**
     * Appends a new empty {@code <colgroup>} to this table.
     *
     * @return the newly created column group.
     */
    public TableColumnGroup addColumnGroup() {
        TableColumnGroup group = new TableColumnGroup();
        getElement().insertChild(columnGroupAppendIndex(), group.getElement());
        columnGroups.add(group);
        return group;
    }

    /**
     * Appends an existing {@code <colgroup>} to this table.
     *
     * @param group
     *            the column group to add.
     * @return the same group, for fluent chaining.
     */
    public TableColumnGroup addColumnGroup(TableColumnGroup group) {
        getElement().insertChild(columnGroupAppendIndex(), group.getElement());
        columnGroups.add(group);
        return group;
    }

    /**
     * Appends a new {@code <colgroup>} populated with the given columns.
     *
     * @param columns
     *            the columns to place inside the new group.
     * @return the newly created column group.
     */
    public TableColumnGroup addColumnGroup(TableColumn... columns) {
        return addColumnGroup(Arrays.asList(columns));
    }

    /**
     * List equivalent of {@link #addColumnGroup(TableColumn...)}.
     *
     * @param columns
     *            the columns to place inside the new group.
     * @return the newly created column group.
     */
    public TableColumnGroup addColumnGroup(
            List<? extends TableColumn> columns) {
        TableColumnGroup group = new TableColumnGroup();
        getElement().insertChild(columnGroupAppendIndex(), group.getElement());
        columnGroups.add(group);
        group.addColumns(columns);
        return group;
    }

    /**
     * Returns the column groups attached to this table, in document order.
     *
     * @return an unmodifiable list of column groups.
     */
    public List<TableColumnGroup> getColumnGroups() {
        return Collections.unmodifiableList(new ArrayList<>(columnGroups));
    }

    /**
     * Removes a column group from this table.
     *
     * @param group
     *            the group to remove.
     */
    public void removeColumnGroup(TableColumnGroup group) {
        if (columnGroups.remove(group)) {
            getElement().removeChild(group.getElement());
        }
    }

    /**
     * Returns the head of this table. Creates a new one if none was present,
     * inserted at the correct position (after the caption and any column
     * groups).
     *
     * @return this table's {@code <thead>} element.
     */
    public TableHead getHead() {
        if (head == null) {
            head = new TableHead();
            getElement().insertChild(headIndex(), head.getElement());
        }
        return head;
    }

    /**
     * Returns the head if one has been set.
     *
     * @return an {@link Optional} containing the head, or empty if none.
     */
    public Optional<TableHead> findHead() {
        return Optional.ofNullable(head);
    }

    /**
     * Remove the head from this table, if present.
     */
    public void removeHead() {
        if (head != null) {
            getElement().removeChild(head.getElement());
            head = null;
        }
    }

    /**
     * Returns the {@code <tfoot>} element of this table. Creates a new one if
     * none was present, appended at the end of the table per the HTML spec.
     *
     * @return the {@code <tfoot>} element of this table.
     */
    public TableFoot getFoot() {
        if (foot == null) {
            foot = new TableFoot();
            getElement().appendChild(foot.getElement());
        }
        return foot;
    }

    /**
     * Returns the foot if one has been set.
     *
     * @return an {@link Optional} containing the foot, or empty if none.
     */
    public Optional<TableFoot> findFoot() {
        return Optional.ofNullable(foot);
    }

    /**
     * Removes the foot from this table, if present.
     */
    public void removeFoot() {
        if (foot != null) {
            getElement().removeChild(foot.getElement());
            foot = null;
        }
    }

    /**
     * Returns the list of {@code <tbody>} elements in this table.
     *
     * @return an unmodifiable list of body elements.
     */
    public List<TableBody> getBodies() {
        return Collections.unmodifiableList(new ArrayList<>(bodies));
    }

    /**
     * Returns the first body element in this table. Creates one if there's
     * none.
     *
     * @return the first {@code <tbody>} element in the table.
     */
    public TableBody getBody() {
        if (bodies.isEmpty()) {
            return addBody();
        }
        return bodies.get(0);
    }

    /**
     * Adds a new body element to the table, positioned after the existing
     * bodies and before the foot (if any).
     *
     * @return the new body.
     */
    public TableBody addBody() {
        TableBody body = new TableBody();
        getElement().insertChild(bodyAppendIndex(), body.getElement());
        bodies.add(body);
        return body;
    }

    /**
     * Removes a body element from the table.
     *
     * @param body
     *            the body component to remove.
     */
    public void removeBody(TableBody body) {
        if (bodies.remove(body)) {
            getElement().removeChild(body.getElement());
        }
    }

    /**
     * Returns every {@link TableRow} in this table — the head's rows, then the
     * rows of each body in order, then the foot's rows — matching the document
     * order exposed by the browser DOM's {@code HTMLTableElement.rows}. Useful
     * for "iterate all rows" or "count rows" cases; for structural work go
     * through {@link #getHead()}, {@link #getBody()} or {@link #getFoot()}
     * directly.
     *
     * @return an unmodifiable list of all rows in the table.
     */
    public List<TableRow> getRows() {
        List<TableRow> all = new ArrayList<>();
        if (head != null) {
            all.addAll(head.getRows());
        }
        for (TableBody body : bodies) {
            all.addAll(body.getRows());
        }
        if (foot != null) {
            all.addAll(foot.getRows());
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Removes every row from this table's head, bodies and foot. The section
     * elements themselves ({@code <thead>}, {@code <tbody>}, {@code <tfoot>})
     * and any column groups are kept; use {@link #removeHead()},
     * {@link #removeBody(TableBody)} or {@link #removeFoot()} to drop those.
     */
    public void removeAllRows() {
        if (head != null) {
            head.removeAllRows();
        }
        for (TableBody body : bodies) {
            body.removeAllRows();
        }
        if (foot != null) {
            foot.removeAllRows();
        }
    }

    /**
     * Appends a new empty row to this table's body, creating an implicit
     * {@code <tbody>} if none exists yet. Mirrors the HTML pattern of placing
     * <code>&lt;tr&gt;</code> elements directly inside a
     * <code>&lt;table&gt;</code> (the browser auto-wraps them in
     * {@code <tbody>}).
     *
     * @return the newly created row.
     */
    public TableRow addRow() {
        return getBody().addRow();
    }

    /**
     * Appends a new row containing the given texts as data cells
     * (<code>&lt;td&gt;</code>) to this table's body, creating an implicit
     * {@code <tbody>} if none exists yet.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public TableRow addRow(String... cellTexts) {
        return addRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public TableRow addRow(List<String> cellTexts) {
        TableRow row = getBody().addRow();
        row.addDataCells(cellTexts);
        return row;
    }

    /**
     * Appends the given rows to this table's body, creating an implicit
     * {@code <tbody>} if none exists yet.
     *
     * @param rows
     *            the rows to add.
     */
    public void addRows(TableRow... rows) {
        getBody().addRows(rows);
    }

    /**
     * List equivalent of {@link #addRows(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addRows(List<? extends TableRow> rows) {
        getBody().addRows(rows);
    }

    /**
     * Appends a new empty row to this table's {@code <thead>}, creating it if
     * none exists yet.
     *
     * @return the newly created row.
     */
    public TableRow addHeaderRow() {
        return getHead().addRow();
    }

    /**
     * Appends a new row containing the given texts as header cells
     * (<code>&lt;th&gt;</code>) to this table's {@code <thead>}, creating it if
     * none exists yet.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return the newly created row.
     */
    public TableRow addHeaderRow(String... cellTexts) {
        return addHeaderRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addHeaderRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return the newly created row.
     */
    public TableRow addHeaderRow(List<String> cellTexts) {
        TableRow row = getHead().addRow();
        row.addHeaderCells(cellTexts);
        return row;
    }

    /**
     * Appends the given rows to this table's {@code <thead>}, creating it if
     * none exists yet.
     *
     * @param rows
     *            the rows to add.
     */
    public void addHeaderRows(TableRow... rows) {
        getHead().addRows(rows);
    }

    /**
     * List equivalent of {@link #addHeaderRows(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addHeaderRows(List<? extends TableRow> rows) {
        getHead().addRows(rows);
    }

    /**
     * Appends a new empty row to this table's {@code <tfoot>}, creating it if
     * none exists yet.
     *
     * @return the newly created row.
     */
    public TableRow addFooterRow() {
        return getFoot().addRow();
    }

    /**
     * Appends a new row containing the given texts as data cells
     * (<code>&lt;td&gt;</code>) to this table's {@code <tfoot>}, creating it if
     * none exists yet.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public TableRow addFooterRow(String... cellTexts) {
        return addFooterRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addFooterRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public TableRow addFooterRow(List<String> cellTexts) {
        TableRow row = getFoot().addRow();
        row.addDataCells(cellTexts);
        return row;
    }

    /**
     * Appends the given rows to this table's {@code <tfoot>}, creating it if
     * none exists yet.
     *
     * @param rows
     *            the rows to add.
     */
    public void addFooterRows(TableRow... rows) {
        getFoot().addRows(rows);
    }

    /**
     * List equivalent of {@link #addFooterRows(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addFooterRows(List<? extends TableRow> rows) {
        getFoot().addRows(rows);
    }

    private int columnGroupAppendIndex() {
        int index = columnGroups.size();
        if (caption != null) {
            index++;
        }
        return index;
    }

    private int headIndex() {
        int index = columnGroups.size();
        if (caption != null) {
            index++;
        }
        return index;
    }

    private int bodyAppendIndex() {
        int index = bodies.size() + columnGroups.size();
        if (caption != null) {
            index++;
        }
        if (head != null) {
            index++;
        }
        return index;
    }

}
