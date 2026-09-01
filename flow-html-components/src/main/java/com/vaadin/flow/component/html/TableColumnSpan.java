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

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.HasElement;

/**
 * A table column definition carrying a {@code span} attribute, i.e. either a
 * <code>&lt;col&gt;</code> or a <code>&lt;colgroup&gt;</code>, implemented by
 * {@link TableColumn} and {@link TableColumnGroup}.
 * <p>
 * A {@code span} tells the browser how many consecutive columns the definition
 * covers, so that one element can carry the styling or attributes of a range of
 * columns.
 *
 * @since 25.3
 */
@NullMarked
public interface TableColumnSpan extends HasElement {

    /**
     * Sets the {@code span} attribute — how many consecutive columns this
     * element covers. The default is {@code 1}. Use it to apply the same
     * styling or attributes across a range of columns without writing one
     * element per column.
     *
     * @param span
     *            a positive integer.
     * @throws IllegalArgumentException
     *             if {@code span} is less than 1.
     */
    default void setSpan(int span) {
        if (span < 1) {
            throw new IllegalArgumentException(
                    "span must be a positive integer value");
        }
        getElement().setAttribute(spanAttribute(), String.valueOf(span));
    }

    /**
     * Returns the value of the {@code span} attribute.
     *
     * @return the current span. Default is 1.
     */
    default int getSpan() {
        String span = getElement().getAttribute(spanAttribute());
        if (span == null) {
            span = "1";
        }
        return Integer.parseInt(span);
    }

    /**
     * Removes the {@code span} attribute, restoring the default value of 1.
     */
    default void resetSpan() {
        getElement().removeAttribute(spanAttribute());
    }

    /**
     * Reports whether this element carries an explicit {@code span}, as opposed
     * to falling back to the default of 1.
     *
     * @return {@code true} if the attribute is set.
     */
    default boolean hasSpan() {
        return getElement().hasAttribute(spanAttribute());
    }

    /**
     * The attribute name, as a method rather than a constant: a field in an
     * interface is implicitly {@code public static final} and cannot be made
     * private, so a constant here would become published API, both on this
     * interface and on every class inheriting it.
     */
    private static String spanAttribute() {
        return "span";
    }
}
