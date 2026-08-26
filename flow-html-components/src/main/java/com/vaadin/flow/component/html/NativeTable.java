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
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HtmlContainer;
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
 * and an optional <code>&lt;tfoot&gt;</code>. Building the table through the
 * structured operations below places each part at the correct position
 * automatically; the generic {@link HtmlContainer#add(Component...)} inherited
 * from {@link HtmlContainer} appends without any such ordering.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/table">MDN:
 *      &lt;table&gt; — The Table element</a>
 * @since 24.5
 */
@Tag(Tag.TABLE)
public class NativeTable extends HtmlContainer
        implements ClickNotifier<NativeTable> {

    private static final int RANK_CAPTION = 0;
    private static final int RANK_COLUMN_GROUP = 1;
    private static final int RANK_HEAD = 2;
    private static final int RANK_BODY = 3;
    private static final int RANK_FOOT = 4;

    /**
     * Creates a new empty table.
     */
    public NativeTable() {
        super();
    }

    /**
     * Creates a new table with the given children components.
     *
     * @param components
     *            the children components.
     */
    public NativeTable(Component... components) {
        super(components);
    }

    /**
     * List equivalent of {@link #NativeTable(Component...)}.
     *
     * @param components
     *            the children components.
     */
    public NativeTable(List<? extends Component> components) {
        super(components.toArray(Component[]::new));
    }

    /**
     * Return the table's caption component. Creates a new instance if no
     * caption is present.
     *
     * @return the table's caption.
     */
    public NativeTableCaption getCaption() {
        return findCaption().orElseGet(() -> {
            NativeTableCaption newCaption = new NativeTableCaption();
            addComponentAtIndex(getInsertionIndex(RANK_CAPTION), newCaption);
            return newCaption;
        });
    }

    /**
     * Returns the caption text for this table, or an empty string if no caption
     * has been set.
     *
     * @return the table's caption text.
     */
    public String getCaptionText() {
        return findCaption().map(NativeTableCaption::getText).orElse("");
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
    public NativeTableCaption addCaption(Component... components) {
        return addCaption(Arrays.asList(components));
    }

    /**
     * List equivalent of {@link #addCaption(Component...)}.
     *
     * @param components
     *            the components to append.
     * @return the caption.
     */
    public NativeTableCaption addCaption(List<? extends Component> components) {
        NativeTableCaption tableCaption = getCaption();
        tableCaption.add(components.toArray(Component[]::new));
        return tableCaption;
    }

    /**
     * Remove the caption from this table.
     */
    public void removeCaption() {
        findCaption().ifPresent(this::remove);
    }

    /**
     * Appends a new empty {@code <colgroup>} to this table.
     *
     * @return the newly created column group.
     */
    public NativeTableColumnGroup addColumnGroup() {
        return addColumnGroup(new NativeTableColumnGroup());
    }

    /**
     * Appends an existing {@code <colgroup>} to this table.
     *
     * @param group
     *            the column group to add.
     * @return the same group, for fluent chaining.
     */
    public NativeTableColumnGroup addColumnGroup(NativeTableColumnGroup group) {
        addComponentAtIndex(getInsertionIndex(RANK_COLUMN_GROUP), group);
        return group;
    }

    /**
     * Appends a new {@code <colgroup>} populated with the given columns.
     *
     * @param columns
     *            the columns to place inside the new group.
     * @return the newly created column group.
     */
    public NativeTableColumnGroup addColumnGroup(NativeTableColumn... columns) {
        return addColumnGroup(Arrays.asList(columns));
    }

    /**
     * List equivalent of {@link #addColumnGroup(NativeTableColumn...)}.
     *
     * @param columns
     *            the columns to place inside the new group.
     * @return the newly created column group.
     */
    public NativeTableColumnGroup addColumnGroup(
            List<? extends NativeTableColumn> columns) {
        return addColumnGroup(new NativeTableColumnGroup(columns));
    }

    /**
     * Returns the column groups attached to this table, in document order.
     *
     * @return an unmodifiable list of column groups.
     */
    public List<NativeTableColumnGroup> getColumnGroups() {
        return ComponentUtil
                .getChildrenOfType(this, NativeTableColumnGroup.class).toList();
    }

    /**
     * Removes a column group from this table.
     *
     * @param group
     *            the group to remove.
     */
    public void removeColumnGroup(NativeTableColumnGroup group) {
        remove(group);
    }

    /**
     * Returns the head of this table. Creates a new one if none was present,
     * inserted at the correct position (after the caption and any column
     * groups).
     *
     * @return this table's {@code <thead>} element.
     */
    public NativeTableHeader getHead() {
        return findHead().orElseGet(() -> {
            NativeTableHeader newHead = new NativeTableHeader();
            addComponentAtIndex(getInsertionIndex(RANK_HEAD), newHead);
            return newHead;
        });
    }

    /**
     * Remove the head from this table, if present.
     */
    public void removeHead() {
        findHead().ifPresent(this::remove);
    }

    /**
     * Returns the {@code <tfoot>} element of this table. Creates a new one if
     * none was present, appended at the end of the table per the HTML spec.
     *
     * @return the {@code <tfoot>} element of this table.
     */
    public NativeTableFooter getFoot() {
        return findFoot().orElseGet(() -> {
            NativeTableFooter newFoot = new NativeTableFooter();
            addComponentAtIndex(getInsertionIndex(RANK_FOOT), newFoot);
            return newFoot;
        });
    }

    /**
     * Removes the foot from this table, if present.
     */
    public void removeFoot() {
        findFoot().ifPresent(this::remove);
    }

    /**
     * Returns the list of {@code <tbody>} elements in this table.
     *
     * @return an unmodifiable list of body elements.
     */
    public List<NativeTableBody> getBodies() {
        return ComponentUtil.getChildrenOfType(this, NativeTableBody.class)
                .toList();
    }

    /**
     * Returns the first body element in this table. Creates one if there's
     * none.
     *
     * @return the first {@code <tbody>} element in the table.
     */
    public NativeTableBody getBody() {
        return ComponentUtil.getFirstChildOfType(this, NativeTableBody.class)
                .orElseGet(this::addBody);
    }

    /**
     * Adds a new body element to the table, positioned after the existing
     * bodies and before the foot (if any).
     *
     * @return the new body.
     */
    public NativeTableBody addBody() {
        NativeTableBody body = new NativeTableBody();
        addComponentAtIndex(getInsertionIndex(RANK_BODY), body);
        return body;
    }

    /**
     * Removes a body element from the table.
     *
     * @param body
     *            the body component to remove.
     */
    public void removeBody(NativeTableBody body) {
        remove(body);
    }

    /**
     * Returns every {@link NativeTableRow} in this table — the head's rows,
     * then the rows of each body in order, then the foot's rows — matching the
     * document order exposed by the browser DOM's
     * {@code HTMLTableElement.rows}. Useful for "iterate all rows" or "count
     * rows" cases; for structural work go through {@link #getHead()},
     * {@link #getBody()} or {@link #getFoot()} directly.
     *
     * @return an unmodifiable list of all rows in the table.
     */
    public List<NativeTableRow> getRows() {
        List<NativeTableRow> all = new ArrayList<>();
        findHead().ifPresent(head -> all.addAll(head.getRows()));
        getBodies().forEach(body -> all.addAll(body.getRows()));
        findFoot().ifPresent(foot -> all.addAll(foot.getRows()));
        return Collections.unmodifiableList(all);
    }

    /**
     * Removes every row from this table's head, bodies and foot. The section
     * elements themselves ({@code <thead>}, {@code <tbody>}, {@code <tfoot>})
     * and any column groups are kept; use {@link #removeHead()},
     * {@link #removeBody(NativeTableBody)} or {@link #removeFoot()} to drop
     * those.
     */
    public void removeAllRows() {
        findHead().ifPresent(NativeTableHeader::removeAllRows);
        getBodies().forEach(NativeTableBody::removeAllRows);
        findFoot().ifPresent(NativeTableFooter::removeAllRows);
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
    public NativeTableRow addRow() {
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
    public NativeTableRow addRow(String... cellTexts) {
        return addRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public NativeTableRow addRow(List<String> cellTexts) {
        return getBody().addRow().addDataCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's body, creating an implicit
     * {@code <tbody>} if none exists yet.
     *
     * @param rows
     *            the rows to add.
     */
    public void addRows(NativeTableRow... rows) {
        getBody().addRows(rows);
    }

    /**
     * List equivalent of {@link #addRows(NativeTableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addRows(List<? extends NativeTableRow> rows) {
        getBody().addRows(rows);
    }

    /**
     * Appends a new empty row to this table's {@code <thead>}, creating it if
     * none exists yet.
     *
     * @return the newly created row.
     */
    public NativeTableRow addHeaderRow() {
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
    public NativeTableRow addHeaderRow(String... cellTexts) {
        return addHeaderRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addHeaderRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return the newly created row.
     */
    public NativeTableRow addHeaderRow(List<String> cellTexts) {
        return getHead().addRow().addHeaderCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's {@code <thead>}, creating it if
     * none exists yet.
     *
     * @param rows
     *            the rows to add.
     */
    public void addHeaderRows(NativeTableRow... rows) {
        getHead().addRows(rows);
    }

    /**
     * List equivalent of {@link #addHeaderRows(NativeTableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addHeaderRows(List<? extends NativeTableRow> rows) {
        getHead().addRows(rows);
    }

    /**
     * Appends a new empty row to this table's {@code <tfoot>}, creating it if
     * none exists yet.
     *
     * @return the newly created row.
     */
    public NativeTableRow addFooterRow() {
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
    public NativeTableRow addFooterRow(String... cellTexts) {
        return addFooterRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addFooterRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the newly created row.
     */
    public NativeTableRow addFooterRow(List<String> cellTexts) {
        return getFoot().addRow().addDataCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's {@code <tfoot>}, creating it if
     * none exists yet.
     *
     * @param rows
     *            the rows to add.
     */
    public void addFooterRows(NativeTableRow... rows) {
        getFoot().addRows(rows);
    }

    /**
     * List equivalent of {@link #addFooterRows(NativeTableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addFooterRows(List<? extends NativeTableRow> rows) {
        getFoot().addRows(rows);
    }

    private Optional<NativeTableCaption> findCaption() {
        return ComponentUtil.getFirstChildOfType(this,
                NativeTableCaption.class);
    }

    private Optional<NativeTableHeader> findHead() {
        return ComponentUtil.getFirstChildOfType(this, NativeTableHeader.class);
    }

    private Optional<NativeTableFooter> findFoot() {
        return ComponentUtil.getFirstChildOfType(this, NativeTableFooter.class);
    }

    /**
     * Returns the index at which a child of the given rank must be inserted to
     * keep the caption, column groups, head, bodies and foot in the order the
     * HTML specification requires: right after the last child of the same or a
     * lower rank.
     */
    private int getInsertionIndex(int rank) {
        List<Component> children = getChildren().toList();
        for (int i = 0; i < children.size(); i++) {
            if (getRank(children.get(i)) > rank) {
                return i;
            }
        }
        return children.size();
    }

    private static int getRank(Component child) {
        if (child instanceof NativeTableCaption) {
            return RANK_CAPTION;
        }
        if (child instanceof NativeTableColumnGroup) {
            return RANK_COLUMN_GROUP;
        }
        if (child instanceof NativeTableHeader) {
            return RANK_HEAD;
        }
        if (child instanceof NativeTableFooter) {
            return RANK_FOOT;
        }
        return RANK_BODY;
    }

}
