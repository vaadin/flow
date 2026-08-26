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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;thead&gt;</code> element — the header
 * section of a {@link NativeTable}.
 * <p>
 * Per the <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG
 * HTML specification</a>, a {@code <thead>} may only contain
 * <code>&lt;tr&gt;</code> elements, so build its content through the
 * {@link NativeTableRowContainer} row operations rather than the generic
 * {@link HtmlContainer#add(Component...)} inherited from {@link HtmlContainer}.
 *
 * @since 24.5
 */
@Tag(Tag.THEAD)
public class NativeTableHeader extends HtmlContainer
        implements NativeTableRowContainer, ClickNotifier<NativeTableHeader> {

    /**
     * Creates a new empty table header.
     */
    public NativeTableHeader() {
        super();
    }

    /**
     * Creates a new table header with the given rows.
     *
     * @param rows
     *            the rows to add.
     */
    public NativeTableHeader(NativeTableRow... rows) {
        super(rows);
    }

    /**
     * List equivalent of {@link #NativeTableHeader(NativeTableRow...)}.
     *
     * @param rows
     *            the rows to add.
     */
    public NativeTableHeader(List<? extends NativeTableRow> rows) {
        super(rows.toArray(NativeTableRow[]::new));
    }
}
