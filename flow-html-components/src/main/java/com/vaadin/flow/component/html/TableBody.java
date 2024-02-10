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
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tbody&gt;</code> element.
 *
 * @since 24.4
 */
@Tag(Tag.TBODY)
public class TableBody extends HtmlComponent
        implements TableRowContainer, ClickNotifier<TableBody> {

    /**
     * Creates a new empty table body.
     */
    public TableBody() {
        super();
    }

    /**
     * Creates a new table body with the given children components.
     *
     * @param components
     *            the children components.
     */
    public TableBody(Component... components) {
        super();
        add(components);
    }
}
