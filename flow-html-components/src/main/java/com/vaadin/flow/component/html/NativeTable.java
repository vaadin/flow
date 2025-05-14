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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;table&gt;</code> element.
 *
 * @since 24.4
 */
@Tag(Tag.TABLE)
public class NativeTable extends HtmlContainer
        implements ClickNotifier<NativeTable> {

    /**
     * The table's caption.
     */
    private NativeTableCaption caption;

    /**
     * The {@code <thead>} element of this table.
     */
    private NativeTableHeader head;

    /**
     * The list of {@code <tbody>} elements of the table.
     */
    private final List<NativeTableBody> bodies = new LinkedList<>();

    /**
     * The {@code <tfoot>} element of this table.
     */
    private NativeTableFooter foot;

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
        if (caption == null) {
            caption = new NativeTableCaption();
            addComponentAsFirst(caption);
        }
        return caption;
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
        if (caption != null) {
            remove(caption);
            caption = null;
        }
    }

    /**
     * Returns the head of this table.
     *
     * @return This table's {@code <thead>} element. Creates a new one if no
     *         element was present.
     */
    public NativeTableHeader getHead() {
        if (head == null) {
            head = new NativeTableHeader();
            int index = caption == null ? 0 : 1;
            addComponentAtIndex(index, head);
        }
        return head;
    }

    /**
     * Remove the head from this table, if present.
     */
    public void removeHead() {
        if (head != null) {
            remove(head);
            head = null;
        }
    }

    /**
     * Returns the {@code <tfoot>} element of this table.
     *
     * @return The {@code <tfoot>} element of this table. Creates a new one if
     *         none was present.
     */
    public NativeTableFooter getFoot() {
        if (foot == null) {
            foot = new NativeTableFooter();
            add(foot);
        }
        return foot;
    }

    /**
     * Removes the foot from this table, if present.
     */
    public void removeFoot() {
        if (foot != null) {
            remove(foot);
            foot = null;
        }
    }

    /**
     * Returns the list of {@code <tbody>} elements in this table.
     *
     * @return the list of table body elements of this table.
     */
    public List<NativeTableBody> getBodies() {
        return new ArrayList<>(bodies);
    }

    /**
     * Returns the first body element in this table. Creates one if there's
     * none.
     *
     * @return the first {@code <tbody>} element in the table. Creates one if
     *         there's none.
     */
    public NativeTableBody getBody() {
        if (bodies.isEmpty()) {
            return addBody();
        }
        return bodies.get(0);
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
        return bodies.get(index);
    }

    /**
     * Adds a new body element to the table.
     *
     * @return The new body.
     */
    public NativeTableBody addBody() {
        NativeTableBody body = new NativeTableBody();
        int index = bodies.size();
        if (caption != null) {
            index++;
        }
        if (head != null) {
            index++;
        }
        addComponentAtIndex(index, body);
        bodies.add(body);
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
        bodies.remove(body);
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

}
