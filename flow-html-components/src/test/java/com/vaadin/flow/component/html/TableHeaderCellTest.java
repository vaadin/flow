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

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableHeaderCellTest extends ComponentTest {
    // Most tests in super class

    @BeforeEach
    @Override
    void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
        whitelistProperty("scope");
        super.setup();
    }

    @Override
    protected void addProperties() {
        super.addProperties();
        // Inherited from TableCell — same semantics as TableDataCell
        addProperty("colspan", int.class, 1, 2, false, false);
        addProperty("rowspan", int.class, 1, 2, false, false);
    }

    @Test
    void scope_unsetByDefault() {
        TableHeaderCell th = (TableHeaderCell) getComponent();
        assertTrue(th.getScope().isEmpty());
    }

    @Test
    void scope_setAndGet() {
        TableHeaderCell th = (TableHeaderCell) getComponent();
        th.setScope(TableHeaderCell.Scope.COL);
        assertEquals(TableHeaderCell.Scope.COL, th.getScope().orElseThrow());
        assertEquals("col", th.getElement().getAttribute("scope"));
    }

    @Test
    void scope_setNullClearsAttribute() {
        TableHeaderCell th = (TableHeaderCell) getComponent();
        th.setScope(TableHeaderCell.Scope.ROW);
        th.setScope(null);
        assertTrue(th.getScope().isEmpty());
        assertEquals(null, th.getElement().getAttribute("scope"));
    }

    @Test
    void colspan_inheritedFromTableCell() {
        TableHeaderCell th = (TableHeaderCell) getComponent();
        assertEquals(1, th.getColspan());
        th.setColspan(3);
        assertEquals(3, th.getColspan());
        assertEquals("3", th.getElement().getAttribute("colspan"));
    }

    @Test
    void rowspan_inheritedFromTableCell() {
        TableHeaderCell th = (TableHeaderCell) getComponent();
        assertEquals(1, th.getRowspan());
        th.setRowspan(2);
        assertEquals(2, th.getRowspan());
        assertEquals("2", th.getElement().getAttribute("rowspan"));
    }
}
