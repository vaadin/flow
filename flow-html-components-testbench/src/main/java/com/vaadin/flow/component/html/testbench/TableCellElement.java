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
package com.vaadin.flow.component.html.testbench;

import com.vaadin.testbench.TestBenchElement;

/**
 * A TestBench element representing either kind of table cell, a
 * <code>&lt;td&gt;</code> or a <code>&lt;th&gt;</code>. It carries no
 * {@code @Element} annotation of its own: it is the type
 * {@link TableRowElement#getCells()} speaks in when the kind of cell does not
 * matter, mirroring {@code TableCell} on the component side.
 *
 * @since 25.3
 */
public class TableCellElement extends TestBenchElement {

    /**
     * Declares {@code resolveRowspan}, which confines a row span to the rows
     * left in its own row group: 0 means all of them, and a span reaching past
     * the end of the group is cut off there. Shared with the grid script in
     * {@link TableElement} so that {@link #getResolvedRowspan()} and
     * {@code getCellCovering} cannot come to differ about the same cell.
     */
    static final String RESOLVE_ROWSPAN = """
            const resolveRowspan = (span, remaining) =>
                    span === 0 ? remaining : Math.min(span, remaining);
            """;

    private static final String RESOLVED_ROWSPAN_SCRIPT = RESOLVE_ROWSPAN + """
            const cell = arguments[0];
            const row = cell.parentElement;
            const group = row.parentElement;
            const rows = Array.from(group.children)
                    .filter(e => e.tagName === 'TR');
            return resolveRowspan(cell.rowSpan,
                    rows.length - rows.indexOf(row));
            """;

    /**
     * Returns the {@code colspan} of this cell, or 1 if it carries none. The
     * value comes from the DOM property, so the browser has already applied the
     * rules: an absent, negative or unparseable {@code colspan} reads as 1, and
     * so does 0, that value having been dropped from HTML.
     *
     * @return the number of columns this cell covers, at least 1.
     */
    public int getColspan() {
        return Integer.parseInt(getDomProperty("colSpan"));
    }

    /**
     * Returns the {@code rowspan} of this cell as written, or 1 if it carries
     * none. The value comes from the DOM property, so an absent, negative or
     * unparseable {@code rowspan} reads as 1 — but 0 is kept, since it is
     * meaningful in HTML and means the cell reaches to the end of its row
     * group. Use {@link #getResolvedRowspan()} for the number of rows that
     * works out to.
     *
     * @return the {@code rowspan} of this cell, where 0 means to the end of the
     *         row group.
     */
    public int getRowspan() {
        return Integer.parseInt(getDomProperty("rowSpan"));
    }

    /**
     * Returns how many rows this cell actually covers, which is what
     * {@link #getRowspan()} says only when the cell neither uses 0 nor reaches
     * past the end of its row group. A {@code rowspan} of 0 resolves to the
     * number of rows left in the {@code <thead>}, {@code <tbody>} or
     * {@code <tfoot>} holding the cell, and a span longer than that group is
     * cut off at its end, because a row span may not cross from one row group
     * into the next.
     * <p>
     * There is no counterpart for {@code colspan}: 0 is not a legal value for
     * it, so {@link #getColspan()} is already the number of columns covered.
     *
     * @return the number of rows this cell covers, at least 1.
     */
    public int getResolvedRowspan() {
        return ((Number) executeScript(RESOLVED_ROWSPAN_SCRIPT, this))
                .intValue();
    }
}
