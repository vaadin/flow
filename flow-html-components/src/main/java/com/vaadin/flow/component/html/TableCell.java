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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;

/**
 * Base class for the two kinds of cell a {@link TableRow} can hold: a
 * {@link TableDataCell} (<code>&lt;td&gt;</code>) and a {@link TableHeaderCell}
 * (<code>&lt;th&gt;</code>). It exists so that operations that do not care
 * which kind a cell is — {@link TableRow#getCells()} and
 * {@link TableRow#removeCell(TableCell)} — have a type to speak in.
 * <p>
 * Cells hold flow content, so this class does extend {@link HtmlContainer}:
 * anything may go inside a cell.
 *
 * @since 25.3
 */
public abstract class TableCell extends HtmlContainer {

    /**
     * Creates a new empty cell.
     */
    protected TableCell() {
        super();
    }

    /**
     * Creates a new cell with the given children.
     *
     * @param components
     *            the children components.
     */
    protected TableCell(Component... components) {
        super(components);
    }
}
