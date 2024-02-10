/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;table&gt;</code> element.
 *
 * @since 24.4
 */
@Tag(Tag.TABLE)
public class NativeTable extends HtmlComponent
        implements HasComponents, ClickNotifier<NativeTable> {

    /**
     * The table's caption.
     */
    private TableCaption caption;

    private TableHeader head;

    /**
     * The list of {@code <tbody>} elements of the table.
     */
    private final List<TableBody> bodies = new LinkedList<>();

    private TableFooter foot;

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
        super();
        add(components);
    }

    /**
     * Return the table's caption component.
     * Creates a new instance if no caption is present.
     *
     * @return the table's caption.
     */
    public TableCaption getCaption() {
        if (caption == null) {
            caption = new TableCaption();
            this.addComponentAsFirst(caption);
        }
        return caption;
    }

    /**
     * Sets the caption for this table.
     *
     * @param text the caption's text.
     */
    public void setCaption(String text) {
        getCaption().setText(text);
    }

    /**
     * Remove the caption from this table.
     */
    public void removeCaption() {
        if (caption != null) {
            remove(caption);
        }
    }

    /**
     * Returns the list of {@code <tbody>} elements in this table.
     *
     * @return the list of table body elements of this table.
     */
    public List<TableBody> getBodies() {
        return new ArrayList<>(bodies);
    }

    /**
     * Returns the first body element in this table.
     * Creates one if there's none.
     *
     * @return the first {@code <tbody>} element in the table.
     * Creates one if there's none.
     */
    public TableBody getBody() {
        if (bodies.isEmpty()) {
            return addBody();
        }
        return bodies.get(0);
    }

    /**
     * Returns the {@code <tbody>} element at a given position relative to other
     * {@code <tbody>} elements.
     *
     * @param index The position of the body element relative to other body
     *              elements.
     * @return The table body component at the given position. If the position
     * is 0 and there are no body elements present, a new one is created and
     * returned.
     */
    public TableBody getBody(int index) {
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
    public TableBody addBody() {
        TableBody body = new TableBody();
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
     * @param body The body component to remove.
     */
    public void removeBody(TableBody body) {
        remove(body);
        bodies.remove(body);
    }

    /**
     * Removes a body element at a given position.
     *
     * @param index The position of the body element to remove.
     */
    public void removeBody(int index) {
        TableBody body = getBody(index);
        removeBody(body);
    }

}
