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

import org.jspecify.annotations.NullMarked;

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
@NullMarked
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
     * <p>
     * Unlike {@link #setRowspan(int)}, zero is not allowed: the "span the rest
     * of the column group" meaning it once had was dropped from HTML, and
     * browsers now clamp it back to {@code 1}. Values above 1000 are clamped to
     * 1000.
     *
     * @param colspan
     *            a positive integer.
     * @throws IllegalArgumentException
     *             if {@code colspan} is less than 1.
     */
    public void setColspan(int colspan) {
        if (colspan < 1) {
            throw new IllegalArgumentException(
                    "colspan must be a positive integer value");
        }
        setSpanAttribute(ATTRIBUTE_COLSPAN, colspan);
    }

    /**
     * Returns the value of the {@code colspan} attribute.
     *
     * @return the current colspan. Default is 1.
     */
    public int getColspan() {
        return getSpan(ATTRIBUTE_COLSPAN);
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
     * <p>
     * Zero is allowed and still means something in HTML: the cell extends to
     * the end of the row group ({@code <thead>}, {@code <tbody>} or
     * {@code <tfoot>}, even an implicit one) it belongs to. Values above 65534
     * are clamped to 65534.
     *
     * @param rowspan
     *            a non-negative integer, where 0 spans the rest of the row
     *            group.
     * @throws IllegalArgumentException
     *             if {@code rowspan} is negative.
     */
    public void setRowspan(int rowspan) {
        if (rowspan < 0) {
            throw new IllegalArgumentException(
                    "rowspan must be a non-negative integer value");
        }
        setSpanAttribute(ATTRIBUTE_ROWSPAN, rowspan);
    }

    /**
     * Returns the value of the {@code rowspan} attribute.
     *
     * @return the current rowspan. Default is 1.
     */
    public int getRowspan() {
        return getSpan(ATTRIBUTE_ROWSPAN);
    }

    /**
     * Resets the rowspan to its default value of 1.
     */
    public void resetRowspan() {
        getElement().removeAttribute(ATTRIBUTE_ROWSPAN);
    }

    private void setSpanAttribute(String attribute, int span) {
        getElement().setAttribute(attribute, String.valueOf(span));
    }

    private int getSpan(String attribute) {
        String span = getElement().getAttribute(attribute);
        return span == null ? 1 : Integer.parseInt(span);
    }
}
