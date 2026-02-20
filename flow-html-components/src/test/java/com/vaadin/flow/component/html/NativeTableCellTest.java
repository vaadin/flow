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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeTableCellTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addProperty("colspan", int.class, 1, 2, false, false);
        addProperty("rowspan", int.class, 1, 2, false, false);
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

}
