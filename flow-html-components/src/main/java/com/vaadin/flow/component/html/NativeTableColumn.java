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
 * Component representing a <code>&lt;col&gt;</code> element — a column (or
 * range of columns, via {@link #setSpan(int)}) inside a
 * {@link NativeTableColumnGroup}. Use it to apply column-wide styling without
 * repeating it on every cell: a class or id on a {@code <col>} can target all
 * the data cells in that column.
 * <p>
 * {@code <col>} is a void element (no children, no end tag) and is only valid
 * inside a {@code <colgroup>} that does not itself carry a {@code span}
 * attribute. Only a limited subset of CSS applies to columns:
 * {@code background}, {@code border} (with {@code border-collapse: collapse}),
 * {@code visibility: collapse} and {@code width}. Text and font properties do
 * not inherit into the cells — style those on <code>&lt;td&gt;</code> or
 * <code>&lt;th&gt;</code> instead.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/col">MDN:
 *      &lt;col&gt; — The Table Column element</a>
 */
@Tag(Tag.COL)
public class NativeTableColumn extends HtmlComponent
        implements NativeTableColumnSpan {

    /**
     * Creates a new column component spanning a single column.
     */
    public NativeTableColumn() {
        super();
    }

    /**
     * Creates a new column component spanning the given number of columns.
     *
     * @param span
     *            the number of consecutive columns this {@code <col>} element
     *            applies to. Must be a positive integer.
     */
    public NativeTableColumn(int span) {
        super();
        setSpan(span);
    }
}
