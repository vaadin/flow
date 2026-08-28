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
 * Component representing a <code>&lt;td&gt;</code> element — a data cell in a
 * {@link TableRow}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/td">MDN:
 *      &lt;td&gt; — The Table Data Cell element</a>
 * @since 25.3
 */
@Tag(Tag.TD)
public class TableDataCell extends TableCell
        implements ClickNotifier<TableDataCell> {

    private static final String ATTRIBUTE_COLSPAN = "colspan";
    private static final String ATTRIBUTE_ROWSPAN = "rowspan";

    /**
     * Creates a new empty data cell.
     */
    public TableDataCell() {
        super();
    }

    /**
     * Creates a new data cell with the given children.
     *
     * @param components
     *            the children components.
     */
    public TableDataCell(Component... components) {
        super(components);
    }

    /**
     * Creates a new data cell with the given text.
     *
     * @param text
     *            the text.
     */
    public TableDataCell(String text) {
        super();
        setText(text);
    }

    /**
     * Creates a new data cell with its text content bound to the given signal.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @see #bindText(Signal)
     */
    public TableDataCell(Signal<String> textSignal) {
        Objects.requireNonNull(textSignal, "textSignal must not be null");
        bindText(textSignal);
    }

    /**
     * Sets the {@code colspan} attribute — how many columns this cell spans.
     * The default is {@code 1}.
     *
     * @param colspan
     *            a non-negative integer.
     */
    public void setColspan(int colspan) {
        if (colspan < 0) {
            throw new IllegalArgumentException(
                    "colspan must be a non-negative integer value");
        }
        getElement().setAttribute(ATTRIBUTE_COLSPAN, String.valueOf(colspan));
    }

    /**
     * Returns the value of the {@code colspan} attribute.
     *
     * @return the current colspan. Default is 1.
     */
    public int getColspan() {
        String colspan = getElement().getAttribute(ATTRIBUTE_COLSPAN);
        if (colspan == null) {
            colspan = "1";
        }
        return Integer.parseInt(colspan);
    }

    /**
     * Resets the colspan to its default value of 1.
     */
    public void resetColspan() {
        getElement().removeAttribute(ATTRIBUTE_COLSPAN);
    }

    /**
     * Sets the {@code rowspan} attribute — how many rows this cell spans. The
     * default is {@code 1}.
     *
     * @param rowspan
     *            a non-negative integer.
     */
    public void setRowspan(int rowspan) {
        if (rowspan < 0) {
            throw new IllegalArgumentException(
                    "rowspan must be a non-negative integer value");
        }
        getElement().setAttribute(ATTRIBUTE_ROWSPAN, String.valueOf(rowspan));
    }

    /**
     * Returns the value of the {@code rowspan} attribute.
     *
     * @return the current rowspan. Default is 1.
     */
    public int getRowspan() {
        String rowspan = getElement().getAttribute(ATTRIBUTE_ROWSPAN);
        if (rowspan == null) {
            rowspan = "1";
        }
        return Integer.parseInt(rowspan);
    }

    /**
     * Resets the rowspan to its default value of 1.
     */
    public void resetRowspan() {
        getElement().removeAttribute(ATTRIBUTE_ROWSPAN);
    }
}
