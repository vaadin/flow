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
 * Common superclass for table cell components ({@link NativeTableCell} and
 * {@link NativeTableHeaderCell}). Provides shared support for the attributes
 * that apply equally to <code>&lt;td&gt;</code> and <code>&lt;th&gt;</code> per
 * the <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG HTML
 * specification</a>: {@code colspan} and {@code rowspan}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/td">MDN:
 *      &lt;td&gt;</a>
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/th">MDN:
 *      &lt;th&gt;</a>
 */
public abstract class AbstractNativeTableCell extends HtmlContainer {

    private static final String ATTRIBUTE_COLSPAN = "colspan";
    private static final String ATTRIBUTE_ROWSPAN = "rowspan";

    /**
     * Creates a new empty cell component.
     */
    protected AbstractNativeTableCell() {
        super();
    }

    /**
     * Creates a new cell with the given children components.
     *
     * @param components
     *            the children components.
     */
    protected AbstractNativeTableCell(Component... components) {
        super(components);
    }

    /**
     * Sets the {@code colspan} attribute — how many columns this cell spans.
     * The default is {@code 1}. Browsers clamp values higher than 1000 back to
     * {@code 1}.
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
     * Sets the {@code rowspan} attribute — how many rows this cell spans. The
     * default is {@code 1}. A value of {@code 0} extends the cell until the end
     * of its grouping section ({@code <thead>}, {@code <tbody>} or
     * {@code <tfoot>}, even if implicitly defined). Browsers clip values above
     * 65534.
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
