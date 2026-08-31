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

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HasComponentsOfType;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;thead&gt;</code> element — the header rows
 * of a table.
 * <p>
 * A {@code <thead>} may only contain rows, so this component exposes the
 * {@link TableRow} operations rather than a generic {@code add(Component)}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/thead">MDN:
 *      &lt;thead&gt;</a>
 * @since 25.3
 */
@NullMarked
@Tag(Tag.THEAD)
public class TableHead extends HtmlComponent implements TableRowContainer,
        HasComponentsOfType<TableRow>, ClickNotifier<TableHead> {

    /**
     * Creates a new empty {@code <thead>}.
     */
    public TableHead() {
        super();
    }

    /**
     * Creates a new {@code <thead>} with the given rows.
     *
     * @param rows
     *            the rows to add.
     */
    public TableHead(TableRow... rows) {
        super();
        add(rows);
    }

    /**
     * List equivalent of {@link #TableHead(TableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public TableHead(List<? extends TableRow> rows) {
        super();
        add(rows.toArray(TableRow[]::new));
    }
}
