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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTest extends ComponentTest {
    // Actual test methods in super class

    @BeforeEach
    @Override
    void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
        // Section accessors look like bean properties to the introspector, but
        // they are components rather than settings
        whitelistProperty("captionText");
        whitelistProperty("caption");
        whitelistProperty("head");
        whitelistProperty("foot");
        super.setup();
    }

    @Test
    @Override
    protected void testHasAriaLabelIsImplemented() {
        // <table> takes aria-label and aria-labelledby; a caption is not
        // always the right way to name a table
        super.testHasAriaLabelIsImplemented();
    }

    private Table table() {
        return (Table) getComponent();
    }

    @Test
    void newTable_hasNoChildren() {
        Table table = table();

        assertEquals(0, table.getChildren().count());
        assertTrue(table.findHead().isEmpty());
        assertTrue(table.findFoot().isEmpty());
        assertTrue(table.getBodies().isEmpty());
    }

    @Test
    void getCaptionText_withoutCaption_isEmptyAndCreatesNothing() {
        Table table = table();

        assertEquals("", table.getCaptionText());
        assertEquals(0, table.getChildren().count());
    }

    @Test
    void setCaptionText_createsTheCaptionAndReadsBack() {
        Table table = table();

        table.setCaptionText("Planets");

        assertEquals("Planets", table.getCaptionText());
        assertEquals(List.of(table.getCaption()), table.getChildren().toList());
    }

    @Test
    void sectionsAreKeptInSpecificationOrder() {
        Table table = table();
        // Created in reverse of the required order to show the result follows
        // the specification, not the calls.
        var foot = table.getFoot();
        var body = table.addBody();
        var head = table.getHead();
        var caption = table.getCaption();

        assertEquals(List.of(caption, head, body, foot),
                table.getChildren().toList());
    }

    @Test
    void addBody_appendsAfterExistingBodiesAndBeforeTheFoot() {
        Table table = table();
        var head = table.getHead();
        var first = table.addBody();
        var foot = table.getFoot();

        var second = table.addBody();

        assertEquals(List.of(head, first, second, foot),
                table.getChildren().toList());
        assertEquals(List.of(first, second), table.getBodies());
    }

    @Test
    void getBody_returnsTheFirstBodyAndCreatesOneWhenThereIsNone() {
        Table table = table();

        var created = table.getBody();
        var second = table.addBody();

        assertEquals(created, table.getBody());
        assertEquals(List.of(created, second), table.getBodies());
    }

    @Test
    void setSections_attachPreBuiltOnesInSpecificationOrder() {
        Table table = table();
        var head = new TableHead(new TableRow(), new TableRow());
        var body = new TableBody(new TableRow());
        var foot = new TableFoot(new TableRow());
        var caption = new TableCaption("Planets");

        table.setFoot(foot);
        table.addBody(body);
        table.setHead(head);
        table.setCaption(caption);

        assertEquals(List.of(caption, head, body, foot),
                table.getChildren().toList());
        assertEquals("Planets", table.getCaptionText());
        assertEquals(2, head.getRows().size());
    }

    @Test
    void setSections_replaceTheOnesAlreadyThere() {
        Table table = table();
        var oldCaption = table.getCaption();
        var oldHead = table.getHead();
        var oldFoot = table.getFoot();
        var caption = new TableCaption("new");
        var head = new TableHead();
        var foot = new TableFoot();

        table.setCaption(caption);
        table.setHead(head);
        table.setFoot(foot);

        assertEquals(List.of(caption, head, foot),
                table.getChildren().toList());
        assertTrue(oldCaption.getParent().isEmpty());
        assertTrue(oldHead.getParent().isEmpty());
        assertTrue(oldFoot.getParent().isEmpty());
    }

    @Test
    void columnGroups_sitAfterTheCaptionAndBeforeTheHead() {
        Table table = table();
        var head = table.getHead();
        var caption = table.getCaption();

        var first = table.addColumnGroup();
        var second = table.addColumnGroup(new TableColumn(2),
                new TableColumn());

        assertEquals(List.of(caption, first, second, head),
                table.getChildren().toList());
        assertEquals(List.of(first, second), table.getColumnGroups());
        assertEquals(2, second.getColumns().size());
    }

    @Test
    void addColumnGroup_listOverloadHoldsTheSameColumns() {
        Table table = table();
        var first = new TableColumn(2);
        var second = new TableColumn();

        var group = table.addColumnGroup(List.of(first, second));

        assertEquals(List.of(group), table.getColumnGroups());
        assertEquals(List.of(first, second), group.getColumns());
    }

    @Test
    void removeColumnGroup_detachesOnlyThatGroup() {
        Table table = table();
        var first = table.addColumnGroup();
        var second = table.addColumnGroup();

        table.removeColumnGroup(first);

        assertEquals(List.of(second), table.getColumnGroups());
        assertTrue(first.getParent().isEmpty());
    }

    @Test
    void addRow_appendsToTheBodyAndCreatesItIfMissing() {
        Table table = table();

        var row = table.addRow("a", "b");

        assertEquals(List.of(row), table.getBody().getRows());
        assertEquals(List.of("a", "b"), row.getDataCells().stream()
                .map(TableDataCell::getText).toList());
    }

    @Test
    void addHeaderAndFooterRows_landInTheirOwnSections() {
        Table table = table();

        var headerRow = table.addHeaderRow("h");
        var footerRow = table.addFooterRow("f");

        assertEquals(List.of(headerRow), table.getHead().getRows());
        assertEquals(List.of(footerRow), table.getFoot().getRows());
        assertEquals(List.of("h"), headerRow.getHeaderCells().stream()
                .map(TableHeaderCell::getText).toList());
        assertEquals(List.of("f"), footerRow.getDataCells().stream()
                .map(TableDataCell::getText).toList());
    }

    @Test
    void getAllRows_returnsHeadThenBodiesThenFootRows() {
        Table table = table();
        // Added out of document order to show the result follows the table,
        // not the calls.
        var footRow = table.addFooterRow();
        var bodyRow = table.addRow();
        var headRow = table.addHeaderRow();
        var secondBodyRow = table.addBody().addRow();

        assertEquals(List.of(headRow, bodyRow, secondBodyRow, footRow),
                table.getAllRows());
    }

    @Test
    void removeAllRows_keepsTheSections() {
        Table table = table();
        table.addHeaderRow();
        table.addRow();
        table.addFooterRow();

        table.removeAllRows();

        assertTrue(table.getAllRows().isEmpty());
        assertEquals(3, table.getChildren().count());
    }

    @Test
    void addRows_listOverloadsAppendLikeTheVarargsOnes() {
        Table table = table();
        var head = new TableRow();
        var headFromList = new TableRow();
        var body = new TableRow();
        var bodyFromList = new TableRow();
        var foot = new TableRow();
        var footFromList = new TableRow();

        table.addHeaderRows(head);
        table.addHeaderRows(List.of(headFromList));
        table.addRows(body);
        table.addRows(List.of(bodyFromList));
        table.addFooterRows(foot);
        table.addFooterRows(List.of(footFromList));

        assertEquals(List.of(head, headFromList, body, bodyFromList, foot,
                footFromList), table.getAllRows());
    }

    @Test
    void addCaption_appendsToTheCaptionAndCreatesItIfMissing() {
        Table table = table();
        var span = new Span("rich");

        var caption = table.addCaption(span);

        assertEquals(caption, table.getCaption());
        assertEquals(List.of(span), caption.getChildren().toList());
        assertEquals(List.of(caption), table.getChildren().toList());
    }

    @Test
    void addCaption_listOverloadAppendsLikeTheVarargsOne() {
        Table table = table();
        var first = new Span("first");
        var second = new Span("second");

        table.addCaption(first);
        var caption = table.addCaption(List.of(second));

        assertEquals(List.of(first, second), caption.getChildren().toList());
        assertEquals(List.of(caption), table.getChildren().toList());
    }

    @Test
    void setSection_null_removesTheOneTheTableHas() {
        Table table = table();
        var caption = table.getCaption();
        var head = table.getHead();
        var foot = table.getFoot();

        table.setCaption(null);
        table.setHead(null);
        table.setFoot(null);

        assertEquals(0, table.getChildren().count());
        assertTrue(caption.getParent().isEmpty());
        assertTrue(head.getParent().isEmpty());
        assertTrue(foot.getParent().isEmpty());

        // and null on a table that has none is a no-op rather than a failure
        table.setCaption(null);
        assertEquals(0, table.getChildren().count());
    }

    @Test
    void getSection_calledTwice_doesNotCreateASecondOne() {
        Table table = table();

        assertEquals(table.getHead(), table.getHead());
        assertEquals(table.getFoot(), table.getFoot());
        assertEquals(table.getCaption(), table.getCaption());
        assertEquals(3, table.getChildren().count());
    }

    @Test
    void removeSections_detachThemAndLetTheAccessorsCreateNewOnes() {
        Table table = table();
        var caption = table.getCaption();
        var head = table.getHead();
        var body = table.addBody();
        var foot = table.getFoot();

        table.removeCaption();
        table.removeHead();
        table.removeBody(body);
        table.removeFoot();

        assertEquals(0, table.getChildren().count());
        assertTrue(caption.getParent().isEmpty());
        assertTrue(head.getParent().isEmpty());
        assertTrue(body.getParent().isEmpty());
        assertTrue(foot.getParent().isEmpty());
        assertTrue(table.getBodies().isEmpty());

        // The accessors create fresh ones rather than handing back the
        // detached components
        assertEquals(List.of(table.getCaption(), table.getHead(),
                table.getBody(), table.getFoot()),
                table.getChildren().toList());
    }
}
