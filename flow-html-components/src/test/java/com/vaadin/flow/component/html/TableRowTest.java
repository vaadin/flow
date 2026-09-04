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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableRowTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        // Component defines no new properties
    }

    @Test
    void addCells_appendThemInCallOrder() {
        TableRow row = new TableRow();

        TableHeaderCell th = row.addHeaderCell();
        TableDataCell td = row.addDataCell();

        assertEquals(List.of(th, td), row.getCells());
        assertEquals(List.of(th), row.getHeaderCells());
        assertEquals(List.of(td), row.getDataCells());
    }

    @Test
    void insertCells_placeThemAtTheGivenPosition() {
        TableRow row = new TableRow();
        TableDataCell first = row.addDataCell();
        TableDataCell last = row.addDataCell();

        TableHeaderCell header = row.insertHeaderCell(1);
        TableDataCell middle = row.insertDataCell(2);
        TableHeaderCell leadingHeader = row.insertHeaderCell(0);
        TableDataCell leadingData = row.insertDataCell(0);

        assertEquals(List.of(leadingData, leadingHeader, first, header, middle,
                last), row.getCells());
    }

    static Stream<Named<Function<List<Component>, TableRow>>> rowBuilders() {
        return Stream.of(
                Named.of("varargs constructor",
                        c -> new TableRow(c.toArray(Component[]::new))),
                Named.of("list constructor", TableRow::new),
                Named.of("varargs addCells", c -> {
                    TableRow row = new TableRow();
                    row.addCells(c.toArray(Component[]::new));
                    return row;
                }), Named.of("list addCells", c -> {
                    TableRow row = new TableRow();
                    row.addCells(c);
                    return row;
                }));
    }

    @ParameterizedTest
    @MethodSource("rowBuilders")
    void constructorAndAddCells_attachPreBuiltCells(
            Function<List<Component>, TableRow> builder) {
        TableHeaderCell th = new TableHeaderCell("Name");
        TableDataCell td = new TableDataCell("Mars");
        TableDataCell appended = new TableDataCell(new Span("rich"));

        TableRow row = builder.apply(List.of(th, td, appended));

        assertEquals(List.of(th, td, appended), row.getCells());
        assertEquals("Name", th.getText());
        assertEquals("Mars", td.getText());
    }

    @Test
    void addComponentAtIndex_placesAPreBuiltCellAtTheGivenPosition() {
        TableRow row = new TableRow();
        TableDataCell first = row.addDataCell();
        TableDataCell last = row.addDataCell();
        TableHeaderCell inserted = new TableHeaderCell("Name");

        row.addComponentAtIndex(1, inserted);

        assertEquals(List.of(first, inserted, last), row.getCells());
    }

    @Test
    void addCells_keepsCellsAsIsAndWrapsAnythingElse() {
        TableRow row = new TableRow();
        TableHeaderCell th = new TableHeaderCell("header");
        TableDataCell td = new TableDataCell("data");
        Span span = new Span("wrapped");

        assertEquals(row, row.addCells(th, td, span));

        List<TableCell> cells = row.getCells();
        assertEquals(th, cells.get(0));
        assertEquals(td, cells.get(1));
        assertEquals(span,
                cells.get(2).getChildren().findFirst().orElseThrow());
    }

    @Test
    void addTextCells_appendOnePerText() {
        TableRow row = new TableRow();

        row.addHeaderCells("a", "b").addDataCells("c", "d");

        assertEquals(List.of("a", "b"), row.getHeaderCells().stream()
                .map(TableHeaderCell::getText).toList());
        assertEquals(List.of("c", "d"), row.getDataCells().stream()
                .map(TableDataCell::getText).toList());
    }

    @Test
    void addColumnHeaderCells_appendOneScopedCellPerText() {
        TableRow row = new TableRow();

        TableRow returned = row.addColumnHeaderCells(List.of("Mon", "Tue"))
                .addColumnHeaderCells("Wed");

        assertEquals(row, returned);
        assertEquals(List.of("Mon", "Tue", "Wed"), row.getHeaderCells().stream()
                .map(TableHeaderCell::getText).toList());
        // Unlike addHeaderCells, each cell labels the column it sits in
        row.getHeaderCells()
                .forEach(cell -> assertEquals(
                        java.util.Optional.of(TableHeaderCell.Scope.COL),
                        cell.getScope()));
    }

    @Test
    void addColumnHeaderCell_setsColScope() {
        TableRow row = new TableRow();

        TableHeaderCell fromText = row.addColumnHeaderCell("Name");
        Span content = new Span("Mass");
        TableHeaderCell fromContent = row.addColumnHeaderCell(content);

        assertEquals("Name", fromText.getText());
        // getText is the element's own text, so a content cell reads as empty
        assertEquals(List.of(content), fromContent.getChildren().toList());
        for (TableHeaderCell cell : List.of(fromText, fromContent)) {
            assertEquals(java.util.Optional.of(TableHeaderCell.Scope.COL),
                    cell.getScope());
        }
    }

    @Test
    void componentTakingCellFactories_holdTheGivenContent() {
        TableRow row = new TableRow();
        Span headerContent = new Span("Name");
        Span dataContent = new Span("Mars");

        TableHeaderCell th = row.addHeaderCell(headerContent);
        TableDataCell td = row.addDataCell(dataContent);

        assertEquals(List.of(headerContent), th.getChildren().toList());
        assertEquals(List.of(dataContent), td.getChildren().toList());
        assertEquals(List.of(th, td), row.getCells());
    }

    @Test
    void addRowHeaderCell_setsRowScope() {
        TableRow row = new TableRow();

        TableHeaderCell cell = row.addRowHeaderCell("Breed");

        assertEquals("Breed", cell.getText());
        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.ROW),
                cell.getScope());
    }

    @Test
    void addGroupHeaderCell_setsTheGroupScopeAndLeavesTheSpanAlone() {
        TableRow row = new TableRow();

        TableHeaderCell rowGroup = row.addRowGroupHeaderCell("Jovian planets");
        TableHeaderCell columnGroup = row
                .addColumnGroupHeaderCell("Measurements");

        assertEquals("Jovian planets", rowGroup.getText());
        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.ROWGROUP),
                rowGroup.getScope());
        assertEquals("Measurements", columnGroup.getText());
        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.COLGROUP),
                columnGroup.getScope());
        // Without an explicit span neither attribute is written out
        assertNull(rowGroup.getElement().getAttribute("rowspan"));
        assertNull(columnGroup.getElement().getAttribute("colspan"));
    }

    @Test
    void scopedHeaderCellFactories_takeContentAsWellAsText() {
        TableRow row = new TableRow();
        Span rowLabel = new Span("Breed");
        Span rowGroupLabel = new Span("Gas giants");
        Span columnGroupLabel = new Span("Measurements");

        TableHeaderCell rowCell = row.addRowHeaderCell(rowLabel);
        TableHeaderCell rowGroupCell = row.addRowGroupHeaderCell(rowGroupLabel);
        TableHeaderCell columnGroupCell = row
                .addColumnGroupHeaderCell(columnGroupLabel);

        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.ROW),
                rowCell.getScope());
        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.ROWGROUP),
                rowGroupCell.getScope());
        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.COLGROUP),
                columnGroupCell.getScope());
        // the content lands in the cell rather than being flattened to text
        assertEquals(List.of(rowLabel), rowCell.getChildren().toList());
        assertEquals(List.of(rowGroupLabel),
                rowGroupCell.getChildren().toList());
        assertEquals(List.of(columnGroupLabel),
                columnGroupCell.getChildren().toList());
        assertEquals(List.of(rowCell, rowGroupCell, columnGroupCell),
                row.getHeaderCells());
    }

    @Test
    void addGroupHeaderCell_withASpan_reachesOverTheGroupItNames() {
        TableRow row = new TableRow();

        TableHeaderCell rowGroup = row.addRowGroupHeaderCell("Gas giants", 2);
        TableHeaderCell columnGroup = row.addColumnGroupHeaderCell("Size", 3);

        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.ROWGROUP),
                rowGroup.getScope());
        assertEquals(2, rowGroup.getRowspan());
        assertEquals(java.util.Optional.of(TableHeaderCell.Scope.COLGROUP),
                columnGroup.getScope());
        assertEquals(3, columnGroup.getColspan());
        // These overloads attach the cell themselves rather than going through
        // addHeaderCell, so pin that they land in the row, in order
        assertEquals(List.of(rowGroup, columnGroup), row.getCells());
    }

    @Test
    void addGroupHeaderCell_rejectsAnInvalidSpanBeforeAttachingTheCell() {
        TableRow row = new TableRow();

        assertThrows(IllegalArgumentException.class,
                () -> row.addRowGroupHeaderCell("Jovian planets", -1));
        assertThrows(IllegalArgumentException.class,
                () -> row.addColumnGroupHeaderCell("Size", 0));

        assertTrue(row.getCells().isEmpty());
    }

    @Test
    void remove_detachesTheCellFromTheRow() {
        TableRow row = new TableRow();
        TableHeaderCell th = row.addHeaderCell();
        TableDataCell td = row.addDataCell();

        row.remove(th);

        assertEquals(List.of(td), row.getCells());
        assertTrue(th.getParent().isEmpty());
    }

    @Test
    void removeAll_leavesTheRowEmpty() {
        TableRow row = new TableRow();
        row.addHeaderCell();
        row.addDataCell();

        row.removeAll();

        assertTrue(row.getCells().isEmpty());
        assertEquals(0, row.getChildren().count());
    }
}
