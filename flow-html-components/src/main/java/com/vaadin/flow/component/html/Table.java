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
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;

/**
 * Component representing a <code>&lt;table&gt;</code> element.
 * <p>
 * Unlike the deprecated {@link NativeTable}, this component does not extend
 * {@link com.vaadin.flow.component.HtmlContainer}, so it has no generic
 * {@code add(Component)}. A <code>&lt;table&gt;</code> may only contain a
 * {@code <caption>}, {@code <colgroup>}, {@code <thead>}, {@code <tbody>} and
 * {@code <tfoot>}, and those are reached through the accessors below, which
 * also keep them in the order the WHATWG HTML specification requires.
 *
 * @see <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG
 *      HTML: Tabular data</a>
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/table">MDN:
 *      &lt;table&gt; — The Table element</a>
 * @since 25.3
 */
@NullMarked
@Tag(Tag.TABLE)
public class Table extends HtmlComponent
        implements ClickNotifier<Table>, HasAriaLabel {

    /**
     * Ranks of the children of a <code>&lt;table&gt;</code>, in the order the
     * HTML specification requires them to appear.
     */
    private static final int RANK_CAPTION = 0;
    private static final int RANK_COLUMN_GROUP = 1;
    private static final int RANK_HEAD = 2;
    private static final int RANK_BODY = 3;
    private static final int RANK_FOOT = 4;

    /**
     * Creates a new empty table.
     */
    public Table() {
        super();
    }

    /**
     * Returns this table's caption, creating one if the table has none.
     *
     * @return the table's {@code <caption>}.
     */
    public TableCaption getCaption() {
        return findCaption()
                .orElseGet(() -> insert(new TableCaption(), RANK_CAPTION));
    }

    /**
     * Returns this table's caption if it has one, without creating it.
     *
     * @return the table's {@code <caption>}, or an empty optional.
     */
    private Optional<TableCaption> findCaption() {
        return ComponentUtil.getFirstChildOfType(this, TableCaption.class);
    }

    /**
     * Returns the text content of this table's caption, or an empty string if
     * the table has no caption. Unlike {@link #getCaption()}, this does not
     * create one.
     *
     * @return the caption text, never {@code null}.
     */
    public String getCaptionText() {
        return findCaption().map(TableCaption::getText).orElse("");
    }

    /**
     * Sets the text content of this table's caption, creating the caption if
     * the table has none.
     *
     * @param text
     *            the caption text.
     */
    public void setCaptionText(String text) {
        getCaption().setText(text);
    }

    /**
     * Puts the given caption on this table, replacing the one it already has,
     * if any.
     *
     * @param caption
     *            the caption to use.
     */
    public void setCaption(TableCaption caption) {
        removeCaption();
        insert(caption, RANK_CAPTION);
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
        TableCaption caption = getCaption();
        caption.add(components.toArray(Component[]::new));
        return caption;
    }

    /**
     * Removes this table's caption, if it has one.
     */
    public void removeCaption() {
        findCaption().ifPresent(this::removeChild);
    }

    /**
     * Appends a new empty {@code <colgroup>} to this table, after the caption
     * and any column groups it already has, and before the {@code <thead>}.
     *
     * @return the new column group.
     */
    public TableColumnGroup addColumnGroup() {
        return addColumnGroup(new TableColumnGroup());
    }

    /**
     * Appends the given {@code <colgroup>} to this table.
     *
     * @param group
     *            the column group to add.
     * @return the given group, for chaining.
     */
    public TableColumnGroup addColumnGroup(TableColumnGroup group) {
        return insert(group, RANK_COLUMN_GROUP);
    }

    /**
     * Appends a new {@code <colgroup>} holding the given columns to this table.
     *
     * @param columns
     *            the columns the new group should hold.
     * @return the new column group.
     */
    public TableColumnGroup addColumnGroup(TableColumn... columns) {
        return addColumnGroup(new TableColumnGroup(columns));
    }

    /**
     * Returns the column groups of this table, in document order.
     *
     * @return the table's {@code <colgroup>} elements.
     */
    public List<TableColumnGroup> getColumnGroups() {
        return ComponentUtil.getChildrenOfType(this, TableColumnGroup.class)
                .toList();
    }

    /**
     * Removes the given column group from this table.
     *
     * @param group
     *            the column group to remove.
     */
    public void removeColumnGroup(TableColumnGroup group) {
        removeChild(group);
    }

    /**
     * Returns this table's {@code <thead>}, creating one if the table has none.
     *
     * @return the table's {@code <thead>}.
     */
    public TableHead getHead() {
        return findHead().orElseGet(() -> insert(new TableHead(), RANK_HEAD));
    }

    /**
     * Returns this table's {@code <thead>} if it has one, without creating it.
     *
     * @return the table's {@code <thead>}, or an empty optional.
     */
    public Optional<TableHead> findHead() {
        return ComponentUtil.getFirstChildOfType(this, TableHead.class);
    }

    /**
     * Puts the given {@code <thead>} on this table, replacing the one it
     * already has, if any.
     *
     * @param head
     *            the head to use.
     */
    public void setHead(TableHead head) {
        removeHead();
        insert(head, RANK_HEAD);
    }

    /**
     * Removes this table's {@code <thead>}, if it has one.
     */
    public void removeHead() {
        findHead().ifPresent(this::removeChild);
    }

    /**
     * Returns this table's {@code <tfoot>}, creating one if the table has none.
     *
     * @return the table's {@code <tfoot>}.
     */
    public TableFoot getFoot() {
        return findFoot().orElseGet(() -> insert(new TableFoot(), RANK_FOOT));
    }

    /**
     * Returns this table's {@code <tfoot>} if it has one, without creating it.
     *
     * @return the table's {@code <tfoot>}, or an empty optional.
     */
    public Optional<TableFoot> findFoot() {
        return ComponentUtil.getFirstChildOfType(this, TableFoot.class);
    }

    /**
     * Puts the given {@code <tfoot>} on this table, replacing the one it
     * already has, if any.
     *
     * @param foot
     *            the foot to use.
     */
    public void setFoot(TableFoot foot) {
        removeFoot();
        insert(foot, RANK_FOOT);
    }

    /**
     * Removes this table's {@code <tfoot>}, if it has one.
     */
    public void removeFoot() {
        findFoot().ifPresent(this::removeChild);
    }

    /**
     * Returns the {@code <tbody>} elements of this table, in document order. A
     * table may have several.
     *
     * @return the table's bodies.
     */
    public List<TableBody> getBodies() {
        return ComponentUtil.getChildrenOfType(this, TableBody.class).toList();
    }

    /**
     * Returns the first {@code <tbody>} of this table, creating one if the
     * table has none.
     *
     * @return the table's first body.
     */
    public TableBody getBody() {
        return ComponentUtil.getFirstChildOfType(this, TableBody.class)
                .orElseGet(this::addBody);
    }

    /**
     * Appends a new {@code <tbody>} to this table, after any bodies it already
     * has and before the {@code <tfoot>}.
     *
     * @return the new body.
     */
    public TableBody addBody() {
        return addBody(new TableBody());
    }

    /**
     * Appends the given {@code <tbody>} to this table, after any bodies it
     * already has and before the {@code <tfoot>}.
     *
     * @param body
     *            the body to add.
     * @return the given body, for chaining.
     */
    public TableBody addBody(TableBody body) {
        return insert(body, RANK_BODY);
    }

    /**
     * Removes the given {@code <tbody>} from this table.
     *
     * @param body
     *            the body to remove.
     */
    public void removeBody(TableBody body) {
        removeChild(body);
    }

    /**
     * Returns every row of this table, in document order: the
     * <code>&lt;thead&gt;</code> rows, then the rows of each
     * <code>&lt;tbody&gt;</code>, then the <code>&lt;tfoot&gt;</code> rows.
     *
     * @return all the rows of this table.
     */
    public List<TableRow> getRows() {
        return sections().flatMap(section -> section.getRows().stream())
                .toList();
    }

    /**
     * Removes every row from this table, leaving its sections in place.
     */
    public void removeAllRows() {
        sections().forEach(TableRowContainer::removeAllRows);
    }

    /**
     * Appends a new empty row to this table's <code>&lt;tbody&gt;</code>,
     * creating the body if the table has none. This mirrors what a browser does
     * with a <code>&lt;tr&gt;</code> written straight inside a
     * <code>&lt;table&gt;</code>.
     *
     * @return the new row.
     */
    public TableRow addRow() {
        return getBody().addRow();
    }

    /**
     * Appends a row of data cells with the given texts to this table's
     * <code>&lt;tbody&gt;</code>.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the new row.
     */
    public TableRow addRow(String... cellTexts) {
        return addRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the new row.
     */
    public TableRow addRow(List<String> cellTexts) {
        return addRow().addDataCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's <code>&lt;tbody&gt;</code>.
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
     * Appends a new empty row to this table's <code>&lt;thead&gt;</code>,
     * creating it if the table has none.
     *
     * @return the new row.
     */
    public TableRow addHeaderRow() {
        return getHead().addRow();
    }

    /**
     * Appends a row of header cells with the given texts to this table's
     * <code>&lt;thead&gt;</code>.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return the new row.
     */
    public TableRow addHeaderRow(String... cellTexts) {
        return addHeaderRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addHeaderRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each header cell.
     * @return the new row.
     */
    public TableRow addHeaderRow(List<String> cellTexts) {
        return addHeaderRow().addHeaderCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's <code>&lt;thead&gt;</code>.
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
     * Appends a new empty row to this table's <code>&lt;tfoot&gt;</code>,
     * creating it if the table has none.
     *
     * @return the new row.
     */
    public TableRow addFooterRow() {
        return getFoot().addRow();
    }

    /**
     * Appends a row of data cells with the given texts to this table's
     * <code>&lt;tfoot&gt;</code>.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the new row.
     */
    public TableRow addFooterRow(String... cellTexts) {
        return addFooterRow(Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addFooterRow(String...)}.
     *
     * @param cellTexts
     *            the text content for each data cell.
     * @return the new row.
     */
    public TableRow addFooterRow(List<String> cellTexts) {
        return addFooterRow().addDataCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's <code>&lt;tfoot&gt;</code>.
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

    /**
     * The row containers of this table — head, bodies and foot — in document
     * order. TableRowContainer is not a Component subtype, so this cannot go
     * through ComponentUtil.getChildrenOfType.
     */
    private Stream<TableRowContainer> sections() {
        return getChildren().filter(TableRowContainer.class::isInstance)
                .map(TableRowContainer.class::cast);
    }

    private void removeChild(Component child) {
        getElement().removeChild(child.getElement());
    }

    /**
     * Inserts the given child at the position its rank calls for, i.e. before
     * the first child that must come after it.
     */
    private <T extends Component> T insert(T child, int rank) {
        List<Element> children = getElement().getChildren().toList();
        int index = children.size();
        for (int i = 0; i < children.size(); i++) {
            if (rankOf(children.get(i)) > rank) {
                index = i;
                break;
            }
        }
        getElement().insertChild(index, child.getElement());
        return child;
    }

    private static int rankOf(Element child) {
        Component component = child.getComponent().orElse(null);
        if (component instanceof TableCaption) {
            return RANK_CAPTION;
        }
        if (component instanceof TableColumnGroup) {
            return RANK_COLUMN_GROUP;
        }
        if (component instanceof TableHead) {
            return RANK_HEAD;
        }
        if (component instanceof TableBody) {
            return RANK_BODY;
        }
        // Anything else, the foot included, sorts last
        return RANK_FOOT;
    }
}
