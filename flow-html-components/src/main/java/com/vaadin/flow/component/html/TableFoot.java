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

import java.util.List;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;tfoot&gt;</code> element (the table foot).
 * <p>
 * Per the <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG
 * HTML specification</a>, a {@code <tfoot>} may only contain
 * <code>&lt;tr&gt;</code> elements. This component therefore extends
 * {@link HtmlComponent} (rather than
 * {@link com.vaadin.flow.component.HtmlContainer}) and exposes only
 * {@link TableRow}-specific operations through {@link TableRowContainer}.
 *
 * @since 25.2
 */
@Tag(Tag.TFOOT)
public class TableFoot extends HtmlComponent
        implements TableRowContainer, ClickNotifier<TableFoot> {

    /**
     * Creates a new empty table foot component.
     */
    public TableFoot() {
        super();
    }

    /**
     * Creates a new table foot with the given rows.
     *
     * @param rows
     *            the rows to add.
     */
    public TableFoot(TableRow... rows) {
        super();
        addRows(rows);
    }

    /**
     * List equivalent of {@link #TableFoot(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public TableFoot(List<? extends TableRow> rows) {
        super();
        addRows(rows);
    }
}
