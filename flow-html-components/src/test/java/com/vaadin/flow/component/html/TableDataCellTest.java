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

class TableDataCellTest extends ComponentTest {
    // Property tests in super class

    @Override
    protected void addProperties() {
        addProperty("colspan", int.class, 1, 2, false, false);
        addProperty("rowspan", int.class, 1, 2, false, false);
        addProperty("headers", String[].class, null, new String[] { "a", "b" },
                true, true);
    }

    @Test
    void setHeaders_writesSpaceJoinedAttributeAndReadsBack() {
        TableDataCell cell = (TableDataCell) getComponent();
        assertTrue(cell.getHeaders().isEmpty());

        cell.setHeaders("name", "age");

        assertEquals("name age", cell.getElement().getAttribute("headers"),
                "headers should be space-joined");
        assertArrayEquals(new String[] { "name", "age" },
                cell.getHeaders().orElseThrow());
    }

    @Test
    void setHeaders_empty_clearsTheAttribute() {
        TableDataCell cell = (TableDataCell) getComponent();
        cell.setHeaders("name");

        cell.setHeaders(new String[0]);

        assertNull(cell.getElement().getAttribute("headers"));
        assertTrue(cell.getHeaders().isEmpty());
    }

    @Test
    void setHeaders_fromHeaderCells_usesTheirIds() {
        TableDataCell cell = (TableDataCell) getComponent();
        TableHeaderCell name = new TableHeaderCell("Name");
        TableHeaderCell age = new TableHeaderCell("Age");
        name.setId("name-h");
        age.setId("age-h");

        cell.setHeaders(name, age);

        assertEquals("name-h age-h", cell.getElement().getAttribute("headers"),
                "headers should reference the cells' ids");
    }

    @Test
    void setHeaders_fromHeaderCellWithoutId_throws() {
        TableDataCell cell = (TableDataCell) getComponent();
        TableHeaderCell withoutId = new TableHeaderCell("Name");

        assertThrows(IllegalArgumentException.class,
                () -> cell.setHeaders(withoutId));
    }

    @Test
    void resetHeaders_removesTheAttribute() {
        TableDataCell cell = (TableDataCell) getComponent();
        cell.setHeaders("name");

        cell.resetHeaders();

        assertNull(cell.getElement().getAttribute("headers"));
    }
}
