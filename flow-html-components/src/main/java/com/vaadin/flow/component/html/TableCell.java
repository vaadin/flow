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

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;td&gt;</code> element.
 *
 * @since 24.4
 */
@Tag(Tag.TD)
public class TableCell extends HtmlContainer
        implements ClickNotifier<TableCell> {

    /**
     * Creates a new empty table cell component.
     */
    public TableCell() {
        super();
    }

    /**
     * Creates a new table cell with the given children components.
     *
     * @param components
     *            the children components.
     */
    public TableCell(Component... components) {
        super(components);
    }

    /**
     * Creates a new table cell with the given text.
     *
     * @param text
     *            the text.
     */
    public TableCell(String text) {
        super();
        setText(text);
    }
}
