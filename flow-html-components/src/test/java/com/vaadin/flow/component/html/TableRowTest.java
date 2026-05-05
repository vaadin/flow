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

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasOrderedComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TableRowTest extends ComponentTest {
    // Most tests in super class

    @Override
    protected void addProperties() {
        // Component defines no new properties
    }

    @Test
    void doesNotExposeGenericAddComponent() {
        TableRow row = (TableRow) getComponent();
        assertFalse(row instanceof HasComponents,
                "TableRow must not implement HasComponents");
        assertFalse(row instanceof HasOrderedComponents,
                "TableRow must not implement HasOrderedComponents");
    }

    @Test
    void constructor_rejectsNonCellChildren() {
        assertThrows(IllegalArgumentException.class,
                () -> new TableRow(new Paragraph("not a cell")));
    }

    @Test
    void constructor_acceptsCells() {
        TableDataCell td = new TableDataCell("data");
        TableHeaderCell th = new TableHeaderCell("hdr");
        TableRow row = new TableRow(th, td);
        assertEquals(2, row.getAllCells().size());
        assertEquals(1, row.getDataCells().size());
        assertEquals(1, row.getHeaderCells().size());
    }

    @Test
    void addDataCell_appendsTd() {
        TableRow row = new TableRow();
        TableDataCell td = row.addDataCell();
        assertEquals(1, row.getDataCells().size());
        assertEquals(td, row.getDataCells().get(0));
    }

    @Test
    void addHeaderCell_appendsTh() {
        TableRow row = new TableRow();
        TableHeaderCell th = row.addHeaderCell("Name");
        assertEquals(1, row.getHeaderCells().size());
        assertEquals(th, row.getHeaderCells().get(0));
        assertEquals("Name", th.getText());
    }

    @Test
    void addDataCells_appendsAll() {
        TableRow row = new TableRow();
        TableRow result = row.addDataCells("a", "b", "c");
        assertEquals(row, result);
        assertEquals(3, row.getDataCells().size());
        assertEquals("a", row.getDataCells().get(0).getText());
        assertEquals("b", row.getDataCells().get(1).getText());
        assertEquals("c", row.getDataCells().get(2).getText());
    }

    @Test
    void addHeaderCells_appendsAll() {
        TableRow row = new TableRow();
        TableRow result = row.addHeaderCells("Name", "Age");
        assertEquals(row, result);
        assertEquals(2, row.getHeaderCells().size());
    }

    @Test
    void addCells_appendsPreBuiltCells() {
        TableRow row = new TableRow();
        TableHeaderCell th = new TableHeaderCell("Name");
        TableDataCell td = new TableDataCell("Alice");
        row.addCells(th, td);
        assertEquals(1, row.getHeaderCells().size());
        assertEquals(1, row.getDataCells().size());
    }

    @Test
    void addCells_rejectsNonCellComponent() {
        TableRow row = new TableRow();
        assertThrows(IllegalArgumentException.class,
                () -> row.addCells(new Paragraph("nope")));
    }
}
