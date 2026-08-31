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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableDataCellTest extends ComponentTest {
    // Property tests in super class

    @Override
    protected void addProperties() {
        addProperty("colspan", int.class, 1, 2, false, false);
        addProperty("rowspan", int.class, 1, 2, false, false);
    }

    @Test
    void spansDefaultToOne() {
        TableDataCell cell = (TableDataCell) getComponent();

        assertEquals(1, cell.getColspan());
        assertEquals(1, cell.getRowspan());
    }

    @Test
    void resetSpans_removeTheAttributes() {
        TableDataCell cell = (TableDataCell) getComponent();
        cell.setColspan(2);
        cell.setRowspan(3);

        cell.resetColspan();
        cell.resetRowspan();

        assertNull(cell.getElement().getAttribute("colspan"));
        assertNull(cell.getElement().getAttribute("rowspan"));
    }

    @Test
    void spans_rejectNegativeValues() {
        TableDataCell cell = (TableDataCell) getComponent();

        assertEquals("colspan must be a positive integer value",
                assertThrows(IllegalArgumentException.class,
                        () -> cell.setColspan(-1)).getMessage());
        assertEquals("rowspan must be a non-negative integer value",
                assertThrows(IllegalArgumentException.class,
                        () -> cell.setRowspan(-1)).getMessage());
    }

    @Test
    void zeroSpan_rejectedForColumnsButNotForRows() {
        TableDataCell cell = (TableDataCell) getComponent();

        // colspan=0 was dropped from HTML and browsers clamp it back to 1
        assertThrows(IllegalArgumentException.class, () -> cell.setColspan(0));

        // rowspan=0 still means "to the end of the row group"
        cell.setRowspan(0);
        assertEquals("0", cell.getElement().getAttribute("rowspan"));
        assertEquals(0, cell.getRowspan());
    }
}
