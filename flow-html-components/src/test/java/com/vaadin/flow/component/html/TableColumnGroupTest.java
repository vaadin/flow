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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableColumnGroupTest extends ComponentTest {
    // Property tests in super class

    @Override
    protected void addProperties() {
        addProperty("span", int.class, 1, 2, false, false);
    }

    @Test
    void addColumn_appendsChild() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        TableColumn col = group.addColumn();
        assertEquals(1, group.getColumns().size());
        assertEquals(group, col.getParent().orElseThrow());
    }

    @Test
    void addColumnWithSpan_appendsChildWithSpan() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        TableColumn col = group.addColumn(2);
        assertEquals(2, col.getSpan());
        assertEquals(1, group.getColumns().size());
    }

    @Test
    void addColumns_appendsExistingColumns() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        TableColumn c1 = new TableColumn();
        TableColumn c2 = new TableColumn(3);
        group.addColumns(c1, c2);
        List<TableColumn> columns = group.getColumns();
        assertEquals(2, columns.size());
        assertEquals(c1, columns.get(0));
        assertEquals(c2, columns.get(1));
    }

    @Test
    void varargsConstructor() {
        TableColumn c1 = new TableColumn();
        TableColumn c2 = new TableColumn(2);
        TableColumnGroup group = new TableColumnGroup(c1, c2);
        assertEquals(2, group.getColumns().size());
    }

    @Test
    void removeColumn() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        TableColumn c = group.addColumn();
        group.removeColumn(c);
        assertTrue(group.getColumns().isEmpty());
        assertTrue(c.getParent().isEmpty());
    }

    @Test
    void removeAllColumns() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        group.addColumn();
        group.addColumn();
        group.addColumn();
        group.removeAllColumns();
        assertTrue(group.getColumns().isEmpty());
    }

    @Test
    void setSpan_writesAttribute() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        group.setSpan(4);
        assertEquals("4", group.getElement().getAttribute("span"));
        assertEquals(4, group.getSpan());
    }

    @Test
    void setSpan_rejectsNonPositive() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        assertThrows(IllegalArgumentException.class, () -> group.setSpan(0));
    }

    @Test
    void resetSpan_removesAttribute() {
        TableColumnGroup group = (TableColumnGroup) getComponent();
        group.setSpan(2);
        group.resetSpan();
        assertNull(group.getElement().getAttribute("span"));
    }
}
