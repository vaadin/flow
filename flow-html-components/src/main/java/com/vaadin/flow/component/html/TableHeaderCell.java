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
import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.signals.Signal;

/**
 * Component representing a <code>&lt;th&gt;</code> element — a cell that labels
 * a group of other cells in a {@link Table}. The exact group is defined by the
 * {@link #setScope(Scope) scope} attribute (which row, column, row group, or
 * column group the header applies to) and/or by
 * {@link TableCell#setHeaders(String...) headers} attributes on the data cells
 * that reference this header by id.
 * <p>
 * Inherits {@code colspan}, {@code rowspan} and {@code headers} support from
 * {@link TableCell}, since those attributes apply equally to
 * <code>&lt;td&gt;</code> and <code>&lt;th&gt;</code>.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/th">MDN:
 *      &lt;th&gt; — The Table Header element</a>
 * @since 25.2
 */
@Tag(Tag.TH)
public class TableHeaderCell extends TableCell
        implements ClickNotifier<TableHeaderCell> {

    /**
     * Creates a new empty header cell component.
     */
    public TableHeaderCell() {
        super();
    }

    /**
     * Creates a new header cell with the given children components.
     *
     * @param components
     *            the children components.
     */
    public TableHeaderCell(Component... components) {
        super(components);
    }

    /**
     * Creates a new header cell with the given text.
     *
     * @param text
     *            the text.
     */
    public TableHeaderCell(String text) {
        super();
        setText(text);
    }

    /**
     * Creates a new header cell with its text content bound to the given
     * signal.
     *
     * @param textSignal
     *            the signal to bind, not {@code null}
     * @see #bindText(Signal)
     */
    public TableHeaderCell(Signal<String> textSignal) {
        Objects.requireNonNull(textSignal, "textSignal must not be null");
        bindText(textSignal);
    }

    /**
     * Defines the cells that a <code>&lt;th&gt;</code> header relates to —
     * effectively the answer to "which other cells does this header label?".
     * Setting it correctly is what lets a screen reader read out the right
     * column or row header when a user navigates into a data cell.
     * <p>
     * If no scope is set (or the value is unrecognized), browsers automatically
     * infer the scope from the table structure. Per the <a href=
     * "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/th#scope">HTML
     * spec</a>, the standard keywords are {@link #ROW row}, {@link #COL col},
     * {@link #ROWGROUP rowgroup}, and {@link #COLGROUP colgroup}; {@link #AUTO
     * auto} writes the literal {@code "auto"} attribute value, which browsers
     * treat the same as omitting the attribute.
     */
    public enum Scope {
        /**
         * The header relates to all cells of the row it belongs to. Use this
         * for a leading <code>&lt;th&gt;</code> that labels its row (e.g. the
         * row's name or category).
         */
        ROW("row"),
        /**
         * The header relates to all cells of the column it belongs to. Use this
         * for a header at the top of a column (the most common case in
         * <code>&lt;thead&gt;</code>).
         */
        COL("col"),
        /**
         * The header belongs to a row group ({@code <tbody>} or
         * {@code <thead>}/{@code <tfoot>}) and relates to all cells in that
         * group.
         */
        ROWGROUP("rowgroup"),
        /**
         * The header belongs to a column group ({@link TableColumnGroup}) and
         * relates to all cells in that group.
         */
        COLGROUP("colgroup"),
        /**
         * Writes the literal {@code "auto"} value. Behaves the same as leaving
         * the attribute unset — the browser infers the scope from the table
         * structure.
         */
        AUTO("auto");

        private final String value;

        Scope(String value) {
            this.value = value;
        }

        /**
         * Returns the attribute value as it appears in the rendered HTML.
         */
        public String getValue() {
            return value;
        }

        static Scope fromValue(String value) {
            if (value == null) {
                return null;
            }
            for (Scope s : values()) {
                if (s.value.equals(value)) {
                    return s;
                }
            }
            return null;
        }
    }

    /**
     * Sets the {@code scope} attribute, declaring which cells this header
     * labels. Critical for accessibility — screen readers use this to announce
     * the right header when a user navigates into a data cell. Pass
     * {@code null} to remove the attribute (browsers will then infer scope from
     * structure).
     *
     * @param scope
     *            the scope, or {@code null} to clear the attribute.
     * @see Scope
     */
    public void setScope(Scope scope) {
        if (scope == null) {
            getElement().removeAttribute("scope");
        } else {
            getElement().setAttribute("scope", scope.getValue());
        }
    }

    /**
     * Returns the {@code scope} attribute value, if set.
     *
     * @return the scope wrapped in an {@link Optional}, or empty if unset (or
     *         the value is unrecognized).
     */
    public Optional<Scope> getScope() {
        return Optional.ofNullable(
                Scope.fromValue(getElement().getAttribute("scope")));
    }
}
