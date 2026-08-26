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
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    static Stream<Named<Function<List<Component>, NativeTableRow>>> rowBuilders() {
        return Stream.of(
                Named.of("varargs constructor",
                        components -> new NativeTableRow(
                                components.toArray(Component[]::new))),
                Named.of("list constructor", NativeTableRow::new),
                Named.of("varargs addCells", components -> {
                    NativeTableRow row = new NativeTableRow();
                    row.addCells(components.toArray(Component[]::new));
                    return row;
                }), Named.of("list addCells", components -> {
                    NativeTableRow row = new NativeTableRow();
                    row.addCells(components);
                    return row;
                }));
    }

    @ParameterizedTest
    @MethodSource("rowBuilders")
    void cellsKeptAsIs_otherComponentsWrappedInDataCell(
            Function<List<Component>, NativeTableRow> builder) {
        NativeTableHeaderCell th = new NativeTableHeaderCell("header");
        NativeTableCell td = new NativeTableCell("data");
        Span span = new Span("wrapped");

        NativeTableRow row = builder.apply(List.of(th, td, span));

        List<Component> children = row.getChildren().toList();
        assertEquals(th, children.get(0));
        assertEquals(td, children.get(1));
        NativeTableCell wrapper = assertInstanceOf(NativeTableCell.class,
                children.get(2));
        assertEquals(span, wrapper.getChildren().findFirst().orElseThrow());
    }

    @Test
    void addEmptyCells_appendThemInCallOrder() {
        NativeTableRow row = new NativeTableRow();

        NativeTableHeaderCell th = row.addHeaderCell();
        NativeTableCell td = row.addDataCell();

        assertEquals(List.of(th, td), row.getCells());
        assertEquals("", th.getText());
        assertEquals("", td.getText());
    }

    @Test
    void insertCells_placeThemAtTheGivenPosition() {
        NativeTableRow row = new NativeTableRow();
        NativeTableCell first = row.addDataCell("first");
        NativeTableCell last = row.addDataCell("last");

        NativeTableHeaderCell header = row.insertHeaderCell(1);
        NativeTableCell middle = row.insertDataCell(2);
        NativeTableHeaderCell leadingHeader = row.insertHeaderCell(0);
        NativeTableCell leadingData = row.insertDataCell(0);

        assertEquals(List.of(leadingData, leadingHeader, first, header, middle,
                last), row.getCells());
    }

    @Test
    void getCells_returnsHeaderAndDataCellsInDocumentOrder() {
        NativeTableRow row = new NativeTableRow();
        NativeTableHeaderCell th = row.addHeaderCell("header");
        NativeTableCell td = row.addDataCell("data");
        row.add(new Span("not a cell"));

        assertEquals(List.of(th, td), row.getCells());
    }

    @Test
    void removeCell_detachesCellFromRow() {
        NativeTableRow row = new NativeTableRow();
        NativeTableHeaderCell th = row.addHeaderCell("header");
        NativeTableCell td = row.addDataCell("data");

        row.removeCell(th);

        assertEquals(List.of(td), row.getCells());
        assertTrue(th.getParent().isEmpty());
    }

    @Test
    void addRowHeaderCell_setsRowScope() {
        NativeTableRow row = new NativeTableRow();

        NativeTableHeaderCell cell = row.addRowHeaderCell("Breed");

        assertEquals("Breed", cell.getText());
        assertEquals(java.util.Optional.of(NativeTableHeaderCell.Scope.ROW),
                cell.getScope());
    }

}
