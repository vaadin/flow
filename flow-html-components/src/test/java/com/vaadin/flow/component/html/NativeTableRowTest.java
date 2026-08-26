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

import com.vaadin.flow.component.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class NativeTableRowTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        // Component defines no new properties
    }

    @Test
    void addDataCells_appendsOneTdPerText() {
        NativeTableRow row = new NativeTableRow();
        assertEquals(row, row.addDataCells("a", "b"));

        List<NativeTableCell> cells = row.getDataCells();
        assertEquals(List.of("a", "b"),
                cells.stream().map(NativeTableCell::getText).toList());
        assertEquals(2, row.getChildren().count());
    }

    @Test
    void addHeaderCells_appendsOneThPerText() {
        NativeTableRow row = new NativeTableRow();
        assertEquals(row, row.addHeaderCells("a", "b"));

        List<NativeTableHeaderCell> cells = row.getHeaderCells();
        assertEquals(List.of("a", "b"),
                cells.stream().map(NativeTableHeaderCell::getText).toList());
        assertEquals(2, row.getChildren().count());
    }

    @Test
    void addCells_wrapsOnlyNonCellComponents() {
        NativeTableRow row = new NativeTableRow();
        NativeTableHeaderCell th = new NativeTableHeaderCell("header");
        NativeTableCell td = new NativeTableCell("data");
        Span span = new Span("wrapped");

        row.addCells(th, td, span);

        List<Component> children = row.getChildren().toList();
        assertEquals(th, children.get(0));
        assertEquals(td, children.get(1));
        NativeTableCell wrapper = assertInstanceOf(NativeTableCell.class,
                children.get(2));
        assertEquals(span, wrapper.getChildren().findFirst().orElseThrow());
    }

}
