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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;

/**
 * Common superclass for table cell components ({@link TableDataCell} and
 * {@link TableHeaderCell}). Provides shared support for the attributes that
 * apply equally to <code>&lt;td&gt;</code> and <code>&lt;th&gt;</code> per the
 * <a href="https://html.spec.whatwg.org/multipage/tables.html">WHATWG HTML
 * specification</a>: {@code colspan}, {@code rowspan} and {@code headers}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/td">MDN:
 *      &lt;td&gt;</a>
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/th">MDN:
 *      &lt;th&gt;</a>
 * @since 25.2
 */
public abstract class TableCell extends HtmlContainer {

    private static final String ATTRIBUTE_COLSPAN = "colspan";
    private static final String ATTRIBUTE_ROWSPAN = "rowspan";
    private static final String ATTRIBUTE_HEADERS = "headers";

    /**
     * Creates a new empty cell component.
     */
    protected TableCell() {
        super();
    }

    /**
     * Creates a new cell with the given children components.
     *
     * @param components
     *            the children components.
     */
    protected TableCell(Component... components) {
        super(components);
    }

    /**
     * List equivalent of {@link #TableCell(Component...)}.
     *
     * @param components
     *            the children components.
     */
    protected TableCell(List<? extends Component> components) {
        super(components.toArray(Component[]::new));
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

    /**
     * Sets the {@code headers} attribute — a list of ids referring to the
     * <code>&lt;th&gt;</code> cells that label this cell. Assistive
     * technologies use it to read out the right headers when navigating complex
     * tables, where {@link TableHeaderCell#setScope(TableHeaderCell.Scope)
     * scope} alone isn't enough to disambiguate.
     * <p>
     * Passing no arguments (or an empty array) removes the attribute.
     *
     * @param ids
     *            the ids of the header cells, in any order.
     */
    public void setHeaders(String... ids) {
        setHeaders(ids == null ? List.of() : Arrays.asList(ids));
    }

    /**
     * List equivalent of {@link #setHeaders(String...)}. An empty list (or
     * {@code null}) clears the attribute.
     *
     * @param ids
     *            the ids of the header cells, in any order.
     */
    public void setHeaders(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            getElement().removeAttribute(ATTRIBUTE_HEADERS);
            return;
        }
        for (String id : ids) {
            Objects.requireNonNull(id, "header id must not be null");
        }
        getElement().setAttribute(ATTRIBUTE_HEADERS, String.join(" ", ids));
    }

    /**
     * Convenience overload that takes header cells directly and uses their
     * {@code id} attributes. Each cell must have an id set.
     *
     * @param headerCells
     *            the header cells whose ids should be referenced.
     * @throws IllegalArgumentException
     *             if any of the given cells does not have an id set.
     */
    public void setHeaders(TableHeaderCell... headerCells) {
        setHeadersByCells(
                headerCells == null ? List.of() : Arrays.asList(headerCells));
    }

    /**
     * List equivalent of {@link #setHeaders(TableHeaderCell...)}.
     *
     * @param headerCells
     *            the header cells whose ids should be referenced.
     * @throws IllegalArgumentException
     *             if any of the given cells does not have an id set.
     */
    public void setHeadersByCells(List<? extends TableHeaderCell> headerCells) {
        if (headerCells == null || headerCells.isEmpty()) {
            getElement().removeAttribute(ATTRIBUTE_HEADERS);
            return;
        }
        List<String> ids = new ArrayList<>(headerCells.size());
        for (TableHeaderCell cell : headerCells) {
            ids.add(cell.getId().orElseThrow(() -> new IllegalArgumentException(
                    "Header cell must have an id to be referenced via the headers attribute")));
        }
        setHeaders(ids);
    }

    /**
     * Returns the IDs of the header cells associated with this cell via the
     * {@code headers} attribute, or an empty {@link Optional} if the attribute
     * is not set.
     *
     * @return the parsed list of header IDs.
     */
    public Optional<String[]> getHeaders() {
        String value = getElement().getAttribute(ATTRIBUTE_HEADERS);
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(value.split("\\s+"));
    }

    /**
     * Removes the {@code headers} attribute from this cell.
     */
    public void resetHeaders() {
        getElement().removeAttribute(ATTRIBUTE_HEADERS);
    }
}
