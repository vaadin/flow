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

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tbody&gt;</code> element — a group of data
 * rows.
 * <p>
 * A {@code <tbody>} may only contain rows, so this component exposes the
 * {@link TableRow} operations rather than a generic {@code add(Component)}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/tbody">MDN:
 *      &lt;tbody&gt;</a>
 * @since 25.3
 */
@Tag(Tag.TBODY)
public class TableBody extends HtmlComponent
        implements TableRowContainer, ClickNotifier<TableBody> {

    /**
     * Creates a new empty {@code <tbody>}.
     */
    public TableBody() {
        super();
    }

    /**
     * Creates a new {@code <tbody>} with the given rows.
     *
     * @param rows
     *            the rows to add.
     */
    public TableBody(TableRow... rows) {
        super();
        addRows(rows);
    }
}
