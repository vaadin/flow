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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;table&gt;</code> element.
 * <p>
 * Per the
 * <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG HTML
 * specification</a>, a {@code <table>} may contain (in order): an optional
 * {@code <caption>}, an optional {@code <thead>}, zero or more {@code <tbody>}
 * elements, and an optional {@code <tfoot>}. This component therefore extends
 * {@link HtmlComponent} (rather than
 * {@link com.vaadin.flow.component.HtmlContainer}) and exposes only the
 * structured operations required to build a valid table — child components are
 * inserted at the correct position automatically.
 *
 * @since 25.2
 */
@Tag(Tag.TABLE)
public class Table extends HtmlComponent implements ClickNotifier<Table> {

    private TableCaption caption;
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
     * Returns the caption text for this table, or an empty string if no
     * caption has been set.
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
     * Remove the caption from this table.
     */
    public void removeCaption() {
        if (caption != null) {
            getElement().removeChild(caption.getElement());
            caption = null;
        }
    }

    /**
     * Returns the head of this table. Creates a new one if none was present,
     * inserted at the correct position (after the caption if any).
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
     * Returns the {@code <tbody>} element at a given position relative to
     * other {@code <tbody>} elements.
     *
     * @param index
     *            the position of the body element relative to other body
     *            elements.
     * @return the table body component at the given position. If the position
     *         is 0 and there are no body elements present, a new one is
     *         created and returned.
     */
    public TableBody getBody(int index) {
        if (index == 0) {
            return getBody();
        }
        return bodies.get(index);
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
     * Removes a body element at a given position.
     *
     * @param index
     *            the position of the body element to remove.
     */
    public void removeBody(int index) {
        removeBody(bodies.get(index));
    }

    /**
     * Removes the first body element in the list of bodies of this table.
     */
    public void removeBody() {
        removeBody(0);
    }

    /**
     * Appends a new empty row to this table's body, creating an implicit
     * {@code <tbody>} if none exists yet. Mirrors the HTML pattern of placing
     * {@code <tr>} elements directly inside a {@code <table>} (the browser
     * auto-wraps them in {@code <tbody>}).
     *
     * @return the newly created row.
     */
    public TableRow addRow() {
        return getBody().addRow();
    }

    /**
     * Appends a new row containing the given texts as data cells
     * ({@code <td>}) to this table's body, creating an implicit
     * {@code <tbody>} if none exists yet.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public TableRow addRow(String... cellTexts) {
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
     * ({@code <th>}) to this table's {@code <thead>}, creating it if none
     * exists yet.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return the newly created row.
     */
    public TableRow addHeaderRow(String... cellTexts) {
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
     * ({@code <td>}) to this table's {@code <tfoot>}, creating it if none
     * exists yet.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public TableRow addFooterRow(String... cellTexts) {
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

    private int headIndex() {
        return caption == null ? 0 : 1;
    }

    private int bodyAppendIndex() {
        int index = bodies.size();
        if (caption != null) {
            index++;
        }
        if (head != null) {
            index++;
        }
        return index;
    }

}
