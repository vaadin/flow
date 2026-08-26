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
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;table&gt;</code> element.
 *
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
     * Return the table's caption component. Creates a new instance if no
     * caption is present.
     *
     * @return the table's caption.
     */
    public NativeTableCaption getCaption() {
        return findCaption().orElseGet(() -> {
            NativeTableCaption caption = new NativeTableCaption();
            addComponentAtIndex(getInsertionIndex(RANK_CAPTION), caption);
            return caption;
        });
    }

    /**
     * Return the caption text for this table.
     *
     * @return the table's caption text.
     */
    public String getCaptionText() {
        return getCaption().getText();
    }

    /**
     * Sets the caption text for this table.
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
     * Returns the head of this table.
     *
     * @return This table's {@code <thead>} element. Creates a new one if no
     *         element was present.
     */
    public NativeTableHeader getHead() {
        return findHead().orElseGet(() -> {
            NativeTableHeader head = new NativeTableHeader();
            addComponentAtIndex(getInsertionIndex(RANK_HEAD), head);
            return head;
        });
    }

    /**
     * Remove the head from this table, if present.
     */
    public void removeHead() {
        findHead().ifPresent(this::remove);
    }

    /**
     * Returns the {@code <tfoot>} element of this table.
     *
     * @return The {@code <tfoot>} element of this table. Creates a new one if
     *         none was present.
     */
    public NativeTableFooter getFoot() {
        return findFoot().orElseGet(() -> {
            NativeTableFooter foot = new NativeTableFooter();
            addComponentAtIndex(getInsertionIndex(RANK_FOOT), foot);
            return foot;
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
     * @return the list of table body elements of this table.
     */
    public List<NativeTableBody> getBodies() {
        return ComponentUtil.getChildrenOfType(this, NativeTableBody.class)
                .collect(Collectors.toList());
    }

    /**
     * Returns the first body element in this table. Creates one if there's
     * none.
     *
     * @return the first {@code <tbody>} element in the table. Creates one if
     *         there's none.
     */
    public NativeTableBody getBody() {
        return ComponentUtil.getFirstChildOfType(this, NativeTableBody.class)
                .orElseGet(this::addBody);
    }

    /**
     * Returns the {@code <tbody>} element at a given position relative to other
     * {@code <tbody>} elements.
     *
     * @param index
     *            The position of the body element relative to other body
     *            elements.
     * @return The table body component at the given position. If the position
     *         is 0 and there are no body elements present, a new one is created
     *         and returned.
     */
    public NativeTableBody getBody(int index) {
        if (index == 0) {
            return getBody();
        }
        return getBodies().get(index);
    }

    /**
     * Adds a new body element to the table.
     *
     * @return The new body.
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
     *            The body component to remove.
     */
    public void removeBody(NativeTableBody body) {
        remove(body);
    }

    /**
     * Removes a body element at a given position.
     *
     * @param index
     *            The position of the body element to remove.
     */
    public void removeBody(int index) {
        NativeTableBody body = getBody(index);
        removeBody(body);
    }

    /**
     * Removes the first body element in the list of bodies of this table.
     */
    public void removeBody() {
        removeBody(0);
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

}
