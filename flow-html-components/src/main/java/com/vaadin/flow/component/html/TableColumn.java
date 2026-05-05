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

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;

/**
 * Component representing a <code>&lt;col&gt;</code> element, used to define
 * column-level styling and attributes inside a {@link TableColumnGroup}. The
 * {@code <col>} element is a void element and may not have any children.
 *
 * @since 25.2
 */
@Tag(Tag.COL)
public class TableColumn extends HtmlComponent {

    private static final String ATTRIBUTE_SPAN = "span";

    /**
     * Creates a new column component spanning a single column.
     */
    public TableColumn() {
        super();
    }

    /**
     * Creates a new column component spanning the given number of columns.
     *
     * @param span
     *            the number of consecutive columns this {@code <col>} element
     *            applies to. Must be a positive integer.
     */
    public TableColumn(int span) {
        super();
        setSpan(span);
    }

    /**
     * Sets the number of consecutive columns this {@code <col>} element applies
     * to.
     *
     * @param span
     *            a positive integer.
     */
    public void setSpan(int span) {
        if (span < 1) {
            throw new IllegalArgumentException(
                    "span must be a positive integer value");
        }
        getElement().setAttribute(ATTRIBUTE_SPAN, String.valueOf(span));
    }

    /**
     * Returns the value of the {@code span} attribute.
     *
     * @return the current span. Default is 1.
     */
    public int getSpan() {
        String span = getElement().getAttribute(ATTRIBUTE_SPAN);
        if (span == null) {
            span = "1";
        }
        return Integer.parseInt(span);
    }

    /**
     * Resets the span to its default value of 1.
     */
    public void resetSpan() {
        getElement().removeAttribute(ATTRIBUTE_SPAN);
    }
}
