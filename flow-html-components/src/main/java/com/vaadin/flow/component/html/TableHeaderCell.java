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
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.signals.Signal;

/**
 * Component representing a <code>&lt;th&gt;</code> element.
 *
 * @since 25.2
 */
@Tag(Tag.TH)
public class TableHeaderCell extends HtmlContainer
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
     * The {@code scope} attribute on a {@code <th>} element specifies which
     * cells the header relates to, helping screen readers convey the table's
     * structure.
     */
    public enum Scope {
        ROW("row"), COL("col"), ROWGROUP("rowgroup"), COLGROUP("colgroup"),
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
     * Sets the {@code scope} attribute. Pass {@code null} to remove it.
     *
     * @param scope
     *            the scope, or {@code null} to clear the attribute.
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
