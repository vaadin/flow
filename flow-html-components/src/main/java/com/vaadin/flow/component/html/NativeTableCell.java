/*
 * Copyright 2000-2025 Vaadin Ltd.
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
public class NativeTableCell extends HtmlContainer
        implements ClickNotifier<NativeTableCell> {

    final String ATTRIBUTE_COLSPAN = "colspan";
    final String ATTRIBUTE_ROWSPAN = "rowspan";

    /**
     * Creates a new empty table cell component.
     */
    public NativeTableCell() {
        super();
    }

    /**
     * Creates a new table cell with the given children components.
     *
     * @param components
     *            the children components.
     */
    public NativeTableCell(Component... components) {
        super(components);
    }

    /**
     * Creates a new table cell with the given text.
     *
     * @param text
     *            the text.
     */
    public NativeTableCell(String text) {
        super();
        setText(text);
    }

    /**
     * Set the colspan of this cell.
     *
     * @param colspan
     *            a non-negative integer value that indicates how many columns
     *            the data cell spans or extends.
     */
    public void setColspan(int colspan) {
        if (colspan < 0) {
            throw new IllegalArgumentException(
                    "colspan must be a non-negative integer value");
        }
        getElement().setAttribute(ATTRIBUTE_COLSPAN, String.valueOf(colspan));
    }

    /**
     * Returns the colspan value of this cell.
     *
     * @return the current value of the colspan attribute. Default is 1.
     */
    public int getColspan() {
        String colspan = getElement().getAttribute(ATTRIBUTE_COLSPAN);
        if (colspan == null) {
            colspan = "1";
        }
        return Integer.parseInt(colspan);
    }

    /**
     * Reset colspan to its default value of 1.
     */
    public void resetColspan() {
        getElement().removeAttribute(ATTRIBUTE_COLSPAN);
    }

    /**
     * Sets the rowspan for this cell.
     *
     * @param rowspan
     *            a non-negative integer value that indicates for how many rows
     *            the data cell spans or extends. If its value is set to 0, it
     *            extends until the end of the table grouping section
     *            ({@code <thead>}, {@code <tbody>}, {@code <tfoot>}, even if
     *            implicitly defined), that the cell belongs to.
     */
    public void setRowspan(int rowspan) {
        if (rowspan < 0) {
            throw new IllegalArgumentException(
                    "rowspan must be a non-negative integer value");
        }
        getElement().setAttribute(ATTRIBUTE_ROWSPAN, String.valueOf(rowspan));
    }

    /**
     * Returns the rowspan value of this cell.
     *
     * @return the current value of the rowspan attribute. Default is 1.
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
