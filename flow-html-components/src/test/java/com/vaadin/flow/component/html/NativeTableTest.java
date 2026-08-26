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
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeTableTest extends ComponentTest {
    // Actual test methods in super class

    @BeforeEach
    @Override
    void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
        whitelistProperty("captionText");
        super.setup();
    }

    @Test
    void getCaption() {
        var component = (NativeTable) getComponent();
        NativeTableCaption caption = component.getCaption();
        AssertUtils.assertEquals(component.getChildren().toList().get(0),
                caption, "Caption does not match");
    }

    @Test
    void addsCaptionAsFirstChild() {
        var component = (NativeTable) getComponent();
        assertEquals(0, component.getChildren().count());
        component.getHead();
        component.addBody();
        component.getFoot();
        var caption = component.getCaption();
        assertEquals(4, component.getChildren().count());
        AssertUtils.assertEquals(caption,
                component.getChildren().findFirst().orElseThrow(),
                "Caption is not the first child");
        AssertUtils.assertEquals(caption.getParent().orElseThrow(), component,
                "Table is not the caption's father");

    }

    @Test
    void setCaptionText() {
        var component = (NativeTable) getComponent();
        String expectedText = "Test caption text.";
        component.setCaptionText(expectedText);
        var caption = component.getCaption();
        assertEquals(expectedText, caption.getText());
    }

    @Test
    void getCaptionText() {
        var component = (NativeTable) getComponent();
        String expectedText = "Test caption text.";
        var caption = component.getCaption();
        caption.setText(expectedText);
        assertEquals(expectedText, component.getCaptionText());
    }

    @Test
    void getCaptionText_withoutCaption_isEmptyAndCreatesNothing() {
        var component = (NativeTable) getComponent();

        assertEquals("", component.getCaptionText());
        assertEquals(0, component.getChildren().count());
    }

    @Test
    void removeCaption() {
        var component = (NativeTable) getComponent();
        var caption = component.getCaption();
        component.removeCaption();
        assertTrue(caption.getParent().isEmpty());
    }

    @Test
    void getHead() {
        var component = (NativeTable) getComponent();
        assertEquals(0, component.getChildren().count());
        NativeTableHeader head = component.getHead();
        AssertUtils.assertEquals(component, head.getParent().orElseThrow(),
                "head was not added");
    }

    @Test
    void addHeadAfterCaption() {
        var component = (NativeTable) getComponent();
        component.getCaption();
        var head = component.getHead();
        assertEquals(2, component.getChildren().count());
        int headIndex = component.getChildren().toList().indexOf(head);
        assertEquals(1, headIndex);
    }

    @Test
    void removeHead() {
        var component = (NativeTable) getComponent();
        NativeTableHeader head = component.getHead();
        component.removeHead();
        assertTrue(head.getParent().isEmpty());
    }

    @Test
    void getFoot() {
        var component = (NativeTable) getComponent();
        assertEquals(0, component.getChildren().count());
        NativeTableFooter footer = component.getFoot();
        AssertUtils.assertEquals(component, footer.getParent().orElseThrow(),
                "footer was not added");
    }

    @Test
    void removeFoot() {
        var component = (NativeTable) getComponent();
        NativeTableFooter footer = component.getFoot();
        component.removeFoot();
        assertTrue(footer.getParent().isEmpty());
    }

    @Test
    void addBody() {
        var component = (NativeTable) getComponent();
        component.addBody();
        assertEquals(1, component.getChildren().count());
        component.addBody();
        assertEquals(2, component.getChildren().count());
    }

    @Test
    void addBodyAfterCaption() {
        var component = (NativeTable) getComponent();
        component.getCaption();
        var body = component.addBody();
        assertEquals(1, component.getChildren().toList().indexOf(body));
    }

    @Test
    void addBodyAfterHeader() {
        var component = (NativeTable) getComponent();
        component.getHead();
        var body = component.addBody();
        assertEquals(1, component.getChildren().toList().indexOf(body));
    }

    @Test
    void addBodyAfterBothCaptionAndHeader() {
        var component = (NativeTable) getComponent();
        component.getCaption();
        component.getHead();
        var body = component.addBody();
        assertEquals(2, component.getChildren().toList().indexOf(body));
    }

    @Test
    void getBody() {
        var component = (NativeTable) getComponent();
        var body = component.getBody();
        assertEquals(1, component.getChildren().count());
        // add a second body
        component.addBody();
        assertEquals(2, component.getChildren().count());
        // subsequent calls should return the same first body
        var secondCallBody = component.getBody();
        AssertUtils.assertEquals(body, secondCallBody,
                "No new body should've been created");
    }

    @Test
    void getBodies() {
        var component = (NativeTable) getComponent();
        for (int i = 0; i < 10; i++) {
            component.addBody();
        }
        List<NativeTableBody> bodies = component.getBodies();
        for (NativeTableBody body : bodies) {
            AssertUtils.assertEquals(component, body.getParent().orElseThrow(),
                    "Body is not a child of table");
        }
    }

    @Test
    void removeBodyByReference() {
        var component = (NativeTable) getComponent();
        var body0 = component.addBody();
        var body1 = component.addBody();
        var body2 = component.addBody();
        component.removeBody(body1);
        assertTrue(body0.getParent().isPresent());
        assertTrue(body1.getParent().isEmpty());
        assertTrue(body2.getParent().isPresent());
    }

    static Stream<Named<Function<List<Component>, NativeTable>>> tableConstructors() {
        return Stream.of(
                Named.of("varargs",
                        children -> new NativeTable(
                                children.toArray(Component[]::new))),
                Named.of("list", NativeTable::new));
    }

    @ParameterizedTest
    @MethodSource("tableConstructors")
    void childrenGivenToConstructor_areFoundByAccessors(
            Function<List<Component>, NativeTable> constructor) {
        var head = new NativeTableHeader();
        var body = new NativeTableBody();
        var table = constructor.apply(List.of(head, body));

        assertEquals(head, table.getHead(), "Head given to the constructor "
                + "should be returned instead of a new one being created");
        assertEquals(List.of(body), table.getBodies());
        assertEquals(2, table.getChildren().count(),
                "No extra section should have been created");
    }

    @Test
    void sectionAddedWithAdd_isFoundByAccessors() {
        var component = (NativeTable) getComponent();
        var body = new NativeTableBody();
        component.add(body);

        assertEquals(List.of(body), component.getBodies());
        assertEquals(body, component.getBody());
        assertEquals(1, component.getChildren().count(),
                "No extra body should have been created");
    }

    @Test
    void removeAll_thenGetHead_returnsAnAttachedHead() {
        var component = (NativeTable) getComponent();
        component.getHead();
        component.removeAll();

        var head = component.getHead();
        assertTrue(head.getParent().isPresent(),
                "Head returned after removeAll must be attached to the table");
        assertEquals(1, component.getChildren().count());
    }

    @Test
    void sectionRemovedWithRemove_thenGetFoot_returnsAnAttachedFoot() {
        var component = (NativeTable) getComponent();
        component.remove(component.getFoot());

        var foot = component.getFoot();
        assertTrue(foot.getParent().isPresent(),
                "Foot returned after remove must be attached to the table");
        assertEquals(1, component.getChildren().count());
    }

    @Test
    void addColumnGroup_insertedAfterCaptionBeforeHead() {
        var table = (NativeTable) getComponent();
        table.getCaption();
        table.getHead();
        var group = table.addColumnGroup();
        var children = table.getChildren().toList();
        assertEquals(table.getCaption(), children.get(0));
        assertEquals(group, children.get(1));
        assertEquals(table.getHead(), children.get(2));
    }

    @Test
    void addColumnGroup_beforeHeadEvenIfHeadAddedLater() {
        var table = (NativeTable) getComponent();
        var group = table.addColumnGroup();
        var head = table.getHead();
        var children = table.getChildren().toList();
        assertEquals(group, children.get(0));
        assertEquals(head, children.get(1));
    }

    @Test
    void addColumnGroup_withColumns() {
        var table = (NativeTable) getComponent();
        var group = table.addColumnGroup(new NativeTableColumn(),
                new NativeTableColumn(2));
        assertEquals(2, group.getColumns().size());
        assertEquals(List.of(group), table.getColumnGroups());
    }

    @Test
    void multipleColumnGroups_appearInInsertionOrder() {
        var table = (NativeTable) getComponent();
        var g1 = table.addColumnGroup();
        var g2 = table.addColumnGroup();
        var children = table.getChildren().toList();
        assertEquals(g1, children.get(0));
        assertEquals(g2, children.get(1));
    }

    @Test
    void removeColumnGroup() {
        var table = (NativeTable) getComponent();
        var g1 = table.addColumnGroup();
        var g2 = table.addColumnGroup();
        table.removeColumnGroup(g1);
        assertEquals(List.of(g2), table.getColumnGroups());
        assertTrue(g1.getParent().isEmpty());
    }

    @Test
    void addBody_afterCaptionColumnGroupsAndHead() {
        // caption + 2 colgroups + thead -> tbody must land at index 4
        var table = (NativeTable) getComponent();
        table.setCaptionText("x");
        table.addColumnGroup();
        table.addColumnGroup();
        table.getHead();
        var body = table.addBody();
        assertEquals(4, table.getChildren().toList().indexOf(body));
    }

    @Test
    void addBody_beforeFoot() {
        var table = (NativeTable) getComponent();
        var foot = table.getFoot();
        var body = table.addBody();
        var children = table.getChildren().toList();
        assertEquals(body, children.get(0));
        assertEquals(foot, children.get(1));
    }

    @Test
    void addRow_withTexts_addsDataCellRowToBody() {
        var table = (NativeTable) getComponent();
        var row = table.addRow("a", "b");

        assertEquals(List.of(row), table.getBody().getRows());
        assertEquals(List.of("a", "b"), row.getDataCells().stream()
                .map(NativeTableCell::getText).toList());
    }

    @Test
    void addHeaderRow_withTexts_addsHeaderCellRowToHead() {
        var table = (NativeTable) getComponent();
        var row = table.addHeaderRow("a", "b");

        assertEquals(List.of(row), table.getHead().getRows());
        assertEquals(List.of("a", "b"), row.getHeaderCells().stream()
                .map(NativeTableHeaderCell::getText).toList());
    }

    @Test
    void addFooterRow_withTexts_addsDataCellRowToFoot() {
        var table = (NativeTable) getComponent();
        var row = table.addFooterRow("a", "b");

        assertEquals(List.of(row), table.getFoot().getRows());
        assertEquals(List.of("a", "b"), row.getDataCells().stream()
                .map(NativeTableCell::getText).toList());
    }

    @Test
    void addRows_listOverloadsAppendLikeTheVarargsOnes() {
        var table = (NativeTable) getComponent();
        var headRow = new NativeTableRow();
        var headRowFromList = new NativeTableRow();
        var bodyRow = new NativeTableRow();
        var bodyRowFromList = new NativeTableRow();
        var footRow = new NativeTableRow();
        var footRowFromList = new NativeTableRow();

        table.addHeaderRows(headRow);
        table.addHeaderRows(List.of(headRowFromList));
        table.addRows(bodyRow);
        table.addRows(List.of(bodyRowFromList));
        table.addFooterRows(footRow);
        table.addFooterRows(List.of(footRowFromList));

        assertEquals(List.of(headRow, headRowFromList),
                table.getHead().getRows());
        assertEquals(List.of(bodyRow, bodyRowFromList),
                table.getBody().getRows());
        assertEquals(List.of(footRow, footRowFromList),
                table.getFoot().getRows());
    }

    @Test
    void getRows_returnsHeadThenBodiesThenFootRows() {
        var table = (NativeTable) getComponent();
        // Added out of document order to show the result follows the table,
        // not the calls.
        var footRow = table.addFooterRow();
        var bodyRow = table.addRow();
        var headRow = table.addHeaderRow();
        var secondBodyRow = table.addBody().addRow();

        assertEquals(List.of(headRow, bodyRow, secondBodyRow, footRow),
                table.getRows());
    }

    @Test
    void removeAllRows_keepsTheSections() {
        var table = (NativeTable) getComponent();
        table.addHeaderRow();
        table.addRow();
        table.addFooterRow();

        table.removeAllRows();

        assertTrue(table.getRows().isEmpty());
        assertEquals(3, table.getChildren().count());
    }

    @Test
    void addCaption_appendsToTheCaptionAndCreatesItIfMissing() {
        var table = (NativeTable) getComponent();
        var span = new Span("rich");

        var caption = table.addCaption(span);

        assertEquals(table.getCaption(), caption);
        assertEquals(span, caption.getChildren().findFirst().orElseThrow());
        assertEquals(caption, table.getChildren().findFirst().orElseThrow());
    }

}
