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

import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.signals.Signal;

/**
 * Component representing a <code>&lt;td&gt;</code> element (a table data cell).
 * Inherits {@code colspan}/{@code rowspan} support from {@link TableCell}.
 *
 * @since 25.2
 */
@Tag(Tag.TD)
public class TableDataCell extends TableCell
        implements ClickNotifier<TableDataCell> {

    /**
     * Creates a new empty table cell component.
     */
    public TableDataCell() {
        super();
    }

    /**
     * Creates a new table cell with the given children components.
     *
     * @param components
     *            the children components.
     */
    public TableDataCell(Component... components) {
        super(components);
    }

    /**
     * Creates a new table cell with the given text.
     *
     * @param text
     *            the text.
     */
    public TableDataCell(String text) {
        super();
        setText(text);
    }

    /**
     * Creates a new table cell with its text content bound to the given signal.
     * <p>
     * While a binding for the text content is active, any attempt to set the
     * text manually throws
     * {@link com.vaadin.flow.signals.BindingActiveException}. The same happens
     * when trying to bind a new Signal while one is already bound.
     * <p>
     * Bindings are lifecycle-aware and only active while this component is in
     * the attached state; they are deactivated while the component is in the
     * detached state.
     *
     * @param textSignal
     *            the signal to bind, not <code>null</code>
     * @see #bindText(Signal)
     */
    public TableDataCell(Signal<String> textSignal) {
        Objects.requireNonNull(textSignal, "textSignal must not be null");
        bindText(textSignal);
    }
}
