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
}
