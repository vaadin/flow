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

import static com.vaadin.flow.component.html.AssertUtils.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeTableCellTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addProperty("colspan", int.class, 1, 2, false, false);
        addProperty("rowspan", int.class, 1, 2, false, false);
        addProperty("headers", String[].class, null, new String[] { "a", "b" },
                true, true);
    }

    @Test
    void colspanMustBeNonNegative() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        assertThrows(IllegalArgumentException.class, () -> cell.setColspan(-1));
    }

    @Test
    void setColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setColspan(2);
        assertEquals("2", cell.getElement().getAttribute("colspan"),
                "Colspan should be 2");
    }

    @Test
    void getDefaultColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        int colspan = cell.getColspan();
        assertEquals(1, colspan, "Default colspan should be 1");
    }

    @Test
    void getColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.getElement().setAttribute("colspan", "2");
        assertEquals(2, cell.getColspan(), "Colspan should be 2");
    }

    @Test
    void resetColspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.getElement().setAttribute("colspan", "2");
        cell.resetColspan();
        assertNull(cell.getElement().getAttribute("colspan"),
                "Element should not have colspan attribute");
    }

    @Test
    void rowspanMustNonNegative() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        assertThrows(IllegalArgumentException.class, () -> cell.setRowspan(-1));
    }

    @Test
    void setRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setRowspan(2);
        assertEquals("2", cell.getElement().getAttribute("rowspan"),
                "Rowspan should be 2");
    }

    @Test
    void getDefaultRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        int rowspan = cell.getRowspan();
        assertEquals(1, rowspan, "Default rowspan should be 1");
    }

    @Test
    void getRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.getElement().setAttribute("rowspan", "2");
        assertEquals(2, cell.getRowspan(), "Rowspan should be 2");
    }

    @Test
    void resetRowspan() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setRowspan(2);
        cell.resetRowspan();
        assertNull(cell.getElement().getAttribute("rowspan"),
                "Element should not have rowspan attribute");
    }

    @Test
    void setHeaders_writesSpaceJoinedAttributeAndReadsBack() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        assertTrue(cell.getHeaders().isEmpty());

        cell.setHeaders("name", "age");

        assertEquals("name age", cell.getElement().getAttribute("headers"),
                "headers should be space-joined");
        assertArrayEquals(new String[] { "name", "age" },
                cell.getHeaders().orElseThrow());
    }

    @Test
    void setHeaders_empty_clearsTheAttribute() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setHeaders("name");

        cell.setHeaders(new String[0]);

        assertNull(cell.getElement().getAttribute("headers"));
        assertTrue(cell.getHeaders().isEmpty());
    }

    @Test
    void setHeaders_fromHeaderCells_usesTheirIds() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        NativeTableHeaderCell name = new NativeTableHeaderCell("Name");
        NativeTableHeaderCell age = new NativeTableHeaderCell("Age");
        name.setId("name-h");
        age.setId("age-h");

        cell.setHeaders(name, age);

        assertEquals("name-h age-h", cell.getElement().getAttribute("headers"),
                "headers should reference the cells' ids");
    }

    @Test
    void setHeaders_fromHeaderCellWithoutId_throws() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        NativeTableHeaderCell withoutId = new NativeTableHeaderCell("Name");

        assertThrows(IllegalArgumentException.class,
                () -> cell.setHeaders(withoutId));
    }

    @Test
    void resetHeaders_removesTheAttribute() {
        NativeTableCell cell = (NativeTableCell) getComponent();
        cell.setHeaders("name");

        cell.resetHeaders();

        assertNull(cell.getElement().getAttribute("headers"));
    }

}
