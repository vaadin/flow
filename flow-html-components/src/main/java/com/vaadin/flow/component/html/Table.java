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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
 * <p>
 * Most code never has to name a section. {@link #addRow()},
 * {@link #addRowWithHeader(String, String...)}, {@link #addHeaderRow()} and
 * {@link #addFooterRow()} create the enclosing {@code <tbody>}, {@code <thead>}
 * or {@code <tfoot>} on demand, and {@link #getHeaderRows()},
 * {@link #getBodyRows()}, {@link #getFooterRows()}, {@link #getAllRows()} and
 * {@link #removeRow(TableRow)} read and remove rows without one either. This
 * mirrors the DOM, where inserting a row into a table materialises the missing
 * row group by itself. Reach for {@link #getHead()} and friends only when you
 * need to address a section as a whole — to style it, or to hand it to
 * {@code bindChildren}.
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
     * Returns this table's caption, creating one if the table has none. Reading
     * through this accessor therefore has a side effect; use
     * {@link #hasCaption()} or {@link #getCaptionText()} to inspect the table
     * without adding a caption to it.
     *
     * @return the table's {@code <caption>}.
     */
    public TableCaption getCaption() {
        return findCaption()
                .orElseGet(() -> insert(new TableCaption(), RANK_CAPTION));
    }

    /**
     * Reports whether this table already has a {@code <caption>}. Unlike
     * {@link #getCaption()}, this does not create one.
     *
     * @return {@code true} if the table has a caption.
     */
    public boolean hasCaption() {
        return findCaption().isPresent();
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
     * Returns the text content of this table's caption, or {@code null} if the
     * table has no caption or the caption holds something other than text.
     * Unlike {@link #getCaption()}, this does not create one.
     * <p>
     * A caption assembled from components has no faithful text representation,
     * and reporting whichever text nodes happen to sit around those components
     * would be misleading, so {@code null} is returned instead. Use
     * {@link #hasCaption()} to tell a table with no caption from one whose
     * caption holds markup.
     *
     * @return the caption text, or {@code null} if there is none to report.
     */
    public @Nullable String getCaptionText() {
        return findCaption().filter(Table::holdsOnlyText)
                .map(TableCaption::getText).orElse(null);
    }

    private static boolean holdsOnlyText(TableCaption caption) {
        return caption.getElement().getChildren().allMatch(Element::isTextNode);
    }

    /**
     * Sets the text content of this table's caption, creating the caption if
     * the table has none.
     * <p>
     * Passing {@code null} removes the caption, as
     * {@link #setCaption( TableCaption)} does, so that whatever
     * {@link #getCaptionText()} reports can be handed straight back.
     *
     * @param text
     *            the caption text, or {@code null} to remove the caption.
     */
    public void setCaptionText(@Nullable String text) {
        if (text == null) {
            removeCaption();
        } else {
            getCaption().setText(text);
        }
    }

    /**
     * Puts the given caption on this table, replacing the one it already has,
     * if any.
     *
     * @param caption
     *            the caption to use, or {@code null} to remove the one the
     *            table has.
     */
    public void setCaption(@Nullable TableCaption caption) {
        removeCaption();
        if (caption != null) {
            insert(caption, RANK_CAPTION);
        }
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
     * List equivalent of {@link #addColumnGroup(TableColumn...)}.
     *
     * @param columns
     *            the columns the new group should hold.
     * @return the new column group.
     */
    public TableColumnGroup addColumnGroup(
            List<? extends TableColumn> columns) {
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
     * Reading through this accessor therefore has a side effect; use
     * {@link #hasHead()} or {@link #getHeaderRows()} to inspect the table
     * without adding a section to it.
     *
     * @return the table's {@code <thead>}.
     */
    public TableHead getHead() {
        return findHead().orElseGet(() -> insert(new TableHead(), RANK_HEAD));
    }

    /**
     * Reports whether this table already has a {@code <thead>}. Unlike
     * {@link #getHead()}, this does not create one.
     *
     * @return {@code true} if the table has a head.
     */
    public boolean hasHead() {
        return findHead().isPresent();
    }

    /**
     * Returns this table's {@code <thead>} if it has one, without creating it.
     *
     * @return the table's {@code <thead>}, or an empty optional.
     */
    private Optional<TableHead> findHead() {
        return ComponentUtil.getFirstChildOfType(this, TableHead.class);
    }

    /**
     * Puts the given {@code <thead>} on this table, replacing the one it
     * already has, if any.
     *
     * @param head
     *            the head to use, or {@code null} to remove the one the table
     *            has.
     */
    public void setHead(@Nullable TableHead head) {
        removeHead();
        if (head != null) {
            insert(head, RANK_HEAD);
        }
    }

    /**
     * Removes this table's {@code <thead>}, if it has one.
     */
    public void removeHead() {
        findHead().ifPresent(this::removeChild);
    }

    /**
     * Returns this table's {@code <tfoot>}, creating one if the table has none.
     * Reading through this accessor therefore has a side effect; use
     * {@link #hasFoot()} or {@link #getFooterRows()} to inspect the table
     * without adding a section to it.
     *
     * @return the table's {@code <tfoot>}.
     */
    public TableFoot getFoot() {
        return findFoot().orElseGet(() -> insert(new TableFoot(), RANK_FOOT));
    }

    /**
     * Reports whether this table already has a {@code <tfoot>}. Unlike
     * {@link #getFoot()}, this does not create one.
     *
     * @return {@code true} if the table has a foot.
     */
    public boolean hasFoot() {
        return findFoot().isPresent();
    }

    /**
     * Returns this table's {@code <tfoot>} if it has one, without creating it.
     *
     * @return the table's {@code <tfoot>}, or an empty optional.
     */
    private Optional<TableFoot> findFoot() {
        return ComponentUtil.getFirstChildOfType(this, TableFoot.class);
    }

    /**
     * Puts the given {@code <tfoot>} on this table, replacing the one it
     * already has, if any.
     *
     * @param foot
     *            the foot to use, or {@code null} to remove the one the table
     *            has.
     */
    public void setFoot(@Nullable TableFoot foot) {
        removeFoot();
        if (foot != null) {
            insert(foot, RANK_FOOT);
        }
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
     * table has none. Reading through this accessor therefore has a side
     * effect; use {@link #getBodies()} or {@link #getBodyRows()} to inspect the
     * table without adding a body to it.
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
     * Returns every row of this table, walking its sections in document order:
     * the <code>&lt;thead&gt;</code> rows, then the rows of each
     * <code>&lt;tbody&gt;</code>, then the <code>&lt;tfoot&gt;</code> rows.
     * <p>
     * The name says {@code All} because this is the only row accessor that
     * crosses section kinds. {@link #getHeaderRows()}, {@link #getBodyRows()}
     * and {@link #getFooterRows()} each stay within one kind, though
     * {@code getBodyRows} does flatten several {@code <tbody>} elements when
     * the table has them, and {@link TableHead#getRows()},
     * {@link TableBody#getRows()} and {@link TableFoot#getRows()} each return
     * the rows of one container. {@link #addRow()} appends to the body.
     *
     * @return all the rows of this table.
     */
    public List<TableRow> getAllRows() {
        return sections().flatMap(section -> section.getRows().stream())
                .toList();
    }

    /**
     * Returns the rows of this table's {@code <thead>}, or an empty list if it
     * has none. Unlike {@link #getHead()}, this does not create the section, so
     * it is safe to call while only reading the table — from a debugger watch,
     * for instance.
     *
     * @return the head's rows, in document order.
     */
    public List<TableRow> getHeaderRows() {
        return findHead().map(TableHead::getRows).orElseGet(List::of);
    }

    /**
     * Returns the rows of this table's {@code <tbody>} elements, in document
     * order, or an empty list if it has none. Unlike {@link #getBody()}, this
     * does not create a body.
     *
     * @return the rows of every body, in document order.
     */
    public List<TableRow> getBodyRows() {
        return getBodies().stream().flatMap(body -> body.getRows().stream())
                .toList();
    }

    /**
     * Returns the rows of this table's {@code <tfoot>}, or an empty list if it
     * has none. Unlike {@link #getFoot()}, this does not create the section.
     *
     * @return the foot's rows, in document order.
     */
    public List<TableRow> getFooterRows() {
        return findFoot().map(TableFoot::getRows).orElseGet(List::of);
    }

    /**
     * Removes the given row from whichever section of this table holds it, so
     * that a caller who added the row through {@link #addRow()} or one of its
     * siblings does not have to know which section that was.
     *
     * @param row
     *            the row to remove.
     * @throws IllegalArgumentException
     *             if the row is not in any section of this table.
     */
    public void removeRow(TableRow row) {
        Objects.requireNonNull(row, "row must not be null");
        for (TableRowContainer section : sections().toList()) {
            if (section.indexOf(row) >= 0) {
                section.remove(row);
                return;
            }
        }
        throw new IllegalArgumentException(
                "The given row is not in any section of this table");
    }

    /**
     * Removes every row from this table, leaving its sections in place.
     */
    public void removeAllRows() {
        sections().forEach(TableRowContainer::removeAll);
    }

    /**
     * Appends a new empty row to this table's <code>&lt;tbody&gt;</code>,
     * creating the body if the table has none. This mirrors what a browser does
     * with a <code>&lt;tr&gt;</code> written straight inside a
     * <code>&lt;table&gt;</code>. Use {@link #addHeaderRow()} or
     * {@link #addFooterRow()} for the other sections; note that
     * {@link #getAllRows()} spans all three.
     * <p>
     * A table may have several {@code <tbody>} elements. This one appends to
     * the first, the same one {@link #getBody()} returns; to append to another,
     * call {@link TableBody#addRow()} on the body you mean, which is what
     * {@link #addBody()} hands back.
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
     * <p>
     * For example, {@code addRow("Mercury", "0.330")} renders as:
     *
     * <pre>{@code
     * <tbody>
     *   <tr>
     *     <td>Mercury</td>
     *     <td>0.330</td>
     *   </tr>
     * </tbody>
     * }</pre>
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
     * Appends a row led by a header cell labelling it, followed by data cells
     * with the given texts. The leading cell is a
     * <code>&lt;th scope="row"&gt;</code>, which is what lets assistive
     * technology announce the right label for each data cell in the row.
     *
     * <p>
     * For example, {@code addRowWithHeader("Venus", "4.87", "12,104")} renders
     * as:
     *
     * <pre>{@code
     * <tbody>
     *   <tr>
     *     <th scope="row">Venus</th>
     *     <td>4.87</td>
     *     <td>12,104</td>
     *   </tr>
     * </tbody>
     * }</pre>
     *
     * @param header
     *            the text of the leading header cell.
     * @param cellTexts
     *            the text content for each data cell after it.
     * @return the new row.
     */
    public TableRow addRowWithHeader(String header, String... cellTexts) {
        return addRowWithHeader(header, Arrays.asList(cellTexts));
    }

    /**
     * List equivalent of {@link #addRowWithHeader(String, String...)}.
     *
     * @param header
     *            the text of the leading header cell.
     * @param cellTexts
     *            the text content for each data cell after it.
     * @return the new row.
     */
    public TableRow addRowWithHeader(String header, List<String> cellTexts) {
        TableRow row = addRow();
        row.addRowHeaderCell(header);
        return row.addDataCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's first <code>&lt;tbody&gt;</code>,
     * creating the body if the table has none. As with {@link #addRow()}, a
     * table with several bodies gets the rows in the first of them.
     *
     * @param rows
     *            the rows to add.
     */
    public void addRows(TableRow... rows) {
        getBody().add(rows);
    }

    /**
     * List equivalent of {@link #addRows(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addRows(List<? extends TableRow> rows) {
        getBody().add(rows.toArray(TableRow[]::new));
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
     * <p>
     * For example, {@code addHeaderRow("Name", "Mass")} renders as:
     *
     * <pre>{@code
     * <thead>
     *   <tr>
     *     <th scope="col">Name</th>
     *     <th scope="col">Mass</th>
     *   </tr>
     * </thead>
     * }</pre>
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
        return addHeaderRow().addColumnHeaderCells(cellTexts);
    }

    /**
     * Appends the given rows to this table's <code>&lt;thead&gt;</code>.
     *
     * @param rows
     *            the rows to add.
     */
    public void addHeaderRows(TableRow... rows) {
        getHead().add(rows);
    }

    /**
     * List equivalent of {@link #addHeaderRows(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addHeaderRows(List<? extends TableRow> rows) {
        getHead().add(rows.toArray(TableRow[]::new));
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
     * <p>
     * For example, {@code addFooterRow("Total", "1234")} renders as:
     *
     * <pre>{@code
     * <tfoot>
     *   <tr>
     *     <td>Total</td>
     *     <td>1234</td>
     *   </tr>
     * </tfoot>
     * }</pre>
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
        getFoot().add(rows);
    }

    /**
     * List equivalent of {@link #addFooterRows(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public void addFooterRows(List<? extends TableRow> rows) {
        getFoot().add(rows.toArray(TableRow[]::new));
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
